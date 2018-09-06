package vocento.devops.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import vocento.devops.plugins.qualitygate.Query;
import vocento.devops.plugins.qualitygate.Result;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Execute a query against Sonar to fetch the quality gate status for a build.  This will look for a sonar status that
 * matches the current build number and that we run in the last minute.  The query will wait up to ten minutes for
 * the results to become available on the sonar server.
 */
public class QueryExecutor {

    //public static final String SONAR_FORMAT_PATH = "api/resources/index?resource=%s&metrics=quality_gate_details";
	public static final String SONAR_FORMAT_PATH = "api/qualitygates/project_status?projectKey=%s";
    public static final int SONAR_CONNECTION_RETRIES = 10;
    public static final int SONAR_PROCESSING_WAIT_TIME = 10000;  // wait time between sonar checks in milliseconds
    public static final int SONAR_WAIT_FOR_RESULT = 30000;

    private final URL sonarURL;
    private final int sonarLookBackSeconds;
    private final int waitForProcessingSeconds;
    private final Log log;

    /**
     * Creates a new executor for running queries against sonar.
     *
     * @param sonarServer              Fully qualified URL to the sonar server
     * @param sonarLookBackSeconds     Amount of time to look back into sonar history for the results of this build
     * @param waitForProcessingSeconds Amount of time to wait for sonar to finish processing the job
     * @param log                      Log for logging  @return Results indicating if the build passed the sonar quality gate checks
     * @throws MalformedURLException If the sonar url is invalid
     */
    public QueryExecutor(String sonarServer, int sonarLookBackSeconds, int waitForProcessingSeconds, Log log) throws MalformedURLException {
        this.sonarURL = new URL(sonarServer);
        this.sonarLookBackSeconds = sonarLookBackSeconds;
        this.waitForProcessingSeconds = waitForProcessingSeconds;
        this.log = log;
    }

    /**
     * Execute the given query on the specified sonar server.
     *
     * @param query The query specifying the project and version of the build
     * @return Result fetched from sonar
     * @throws SonarBreakException If there is an issues communicating with sonar
     * @throws IOException         If the url is invalid
     */
    public Result execute(Query query) throws SonarBreakException, IOException {
        URL queryURL = buildURL(sonarURL, query);
        log.info("Built a sonar query url of: " + queryURL.toString());

        if (!isURLAvailable(sonarURL, SONAR_CONNECTION_RETRIES)) {
            throw new SonarBreakException(String.format("Unable to get a valid response after %d tries", SONAR_CONNECTION_RETRIES));
        }

        return fetchSonarStatusWithRetries(queryURL, query.getVersion());
    }

    /**
     * Creates a url for the specified quality gate query
     *
     * @param sonarURL The sonar server we will be querying
     * @param query    Holds details on the query we want to make
     * @return A URL object representing the query
     * @throws MalformedURLException    If the sonar url is not valid
     * @throws IllegalArgumentException If the sonar key is not valid
     */
    protected static URL buildURL(URL sonarURL, Query query) throws MalformedURLException, IllegalArgumentException {
        if (query.getSonarKey() == null || query.getSonarKey().length() == 0) {
            throw new IllegalArgumentException("No resource specified in the Query");
        }
        String sonarPathWithResource = String.format(SONAR_FORMAT_PATH, query.getSonarKey());
        return new URL(sonarURL, sonarPathWithResource);
    }

    /**
     * Get the status from sonar for the currently executing build.  This waits for sonar to complete its processing
     * before returning the results.
     *
     * @param queryURL The sonar URL to get the results from
     * @param version  The current project version number
     * @return Matching result object for this build
     * @throws IOException
     * @throws SonarBreakException
     */
    private Result fetchSonarStatusWithRetries(URL queryURL, String version) throws IOException, SonarBreakException {
        DateTime oneMinuteAgo = DateTime.now().minusSeconds(sonarLookBackSeconds);
        DateTime waitUntil = DateTime.now().plusSeconds(waitForProcessingSeconds);
    	// Esperamos 60 segundos y obtenemos el resultado
        try {
            Thread.sleep(SONAR_WAIT_FOR_RESULT);
        } catch (InterruptedException e) {
            // Do nothing
        }
        if (isURLAvailable(queryURL, 1)) {
            Result result = fetchSonarStatus(queryURL);
            log.info("Found a sonar job run that matches version and in the correct time frame");
            return result;
        }
        String message = "Timed out!!";
        throw new SonarBreakException(message);
    }

    /**
     * Get the status of a build project from sonar.  This returns the current status that sonar has and does not
     * do any checking to ensure it matches the current project
     *
     * @param queryURL The sonar URL to hit to get the status
     * @return The sonar response include quality gate status
     * @throws IOException
     * @throws SonarBreakException
     */
    private Result fetchSonarStatus(URL queryURL) throws IOException, SonarBreakException {
        InputStream in = null;
        try {
            URLConnection connection = queryURL.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            in = connection.getInputStream();
            String response = IOUtils.toString(in);
            return parseResponse(response);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Pings a HTTP URL. This effectively sends a HEAD request and returns <code>true</code> if the response code is in
     * the 200-399 range.
     *
     * @param url        The HTTP URL to be pinged.
     * @param retryCount How many times to check for sonar before giving up
     * @return <code>true</code> if the given HTTP URL has returned response code 200-399 on a HEAD request,
     * otherwise <code>false</code>.
     * @throws IOException If the sonar server is not available
     */
    protected boolean isURLAvailable(URL url, int retryCount) throws IOException {
        boolean serviceFound = false;
        for (int i = 0; i < retryCount; i++) {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            if (200 <= responseCode && responseCode <= 399) {
                log.info(String.format("Got a valid response of %d from %s", responseCode, url));
                serviceFound = true;
                break;
            } else if (i + 1 < retryCount) { // only sleep if not on last iteration
                try {
                    log.info("Sleeping while waiting for sonar to become available");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
        return serviceFound;
    }

    /**
     * Parses the string response from sonar into POJOs.
     *
     * @param response The json response from the sonar server.
     * @return Object representing the Sonar response
     * @throws SonarBreakException Thrown if the response is not JSON or it does not contain quality gate data.
     */
    protected static Result parseResponse(String response) throws SonarBreakException, JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode resJson = mapper.readTree(response).get("projectStatus").get("status");
        Result res = new Result();
        res.setStatus(resJson.textValue());
        return res;
    }

}
