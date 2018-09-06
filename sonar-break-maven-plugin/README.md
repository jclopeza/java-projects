# sonar-break-maven-plugin

## Motivación

Se pretende realizar análisis estático de código con SonarQube en los distintos proyectos y condicionar el resultado de la compilación/construcción dependiendo del resultado de las QualityGates que se definan por cada tipología de proyecto.

Una vez hecho el análisis, este plugin para Maven consultará vía API-REST a SonarQube para ver si se superaron o no las QualityGates definidas.