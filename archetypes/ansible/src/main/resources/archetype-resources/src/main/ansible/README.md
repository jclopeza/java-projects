----------

# Proyecto de despliegue para la aplicación '${application}'

----------

#[[
## Introducción
]]#

Este proyecto Ansible se encarga de realizar el **despliegue** y gestionar la **configuración** para todos los componentes de la aplicación `${application}`.

Los distintos componentes de "${application}" deben generarse en lo que se denomina la `commit stage`, almacenarse en **[Nexus](https://nexus.srv.vocento.in/)** (nuestro repositorio de artefactos) y **de [Nexus](https://nexus.srv.vocento.in/) deben obtenerse para ejecutar cualquier despliegue en cualquiera de los entornos**. 

![03-basic-pipeline](https://bitbucket.vocento.com/projects/AR/repos/devops-guides/raw/img/03-basic-pipeline.png?at=refs%2Fheads%2Fmaster)

#[[
## ¿Cómo identificamos los componentes a desplegar?
]]#

Es fundamental habilitar un **alto grado de visibilidad y trazabilidad** sobre los distintos componentes que componen la versión de la aplicación que vamos a desplegar en un entorno determinado. **Debemos saber:**

* Qué nueva `funcionalidad` implementa el nuevo software que vamos a desplegar.
* Qué `código fuente` se utilizó para generar esa nueva pieza de software.
* Dónde se *cocinó*, es decir, qué `job de construcción` o compilación de **[Jenkins](https://jenkins2.srv.vocento.in/)** generó el artefacto.
* Qué resultado se obtuvo tras la ejecución de las `pruebas automatizadas` y de los `análisis` hechos con **[Sonar](https://sonar.srv.vocento.in/)**.
* Dónde está ubicado el artefacto en **[Nexus](https://nexus.srv.vocento.in/)** y cuál es su `URL de acceso`.
* Cómo ha evolucionado esa pieza de software por el `deployment pipeline` definido (en qué entornos se ha desplegado, cuándo, por quién, quién autorizó el despliegue, etc.). 

Estos `metadatos` asociados al software, se generan durante la `commit stage` y quedan almacenados en la base de datos **[PDS](https://pds.srv.vocento.in/)**. 

![04-nexus-pds](https://bitbucket.vocento.com/projects/AR/repos/devops-guides/raw/img/04-nexus-pds.png?at=refs%2Fheads%2Fmaster)

**A la hora de hacer el despliegue, es fundamental conocer la URL de acceso de cada uno de los artefactos que componen la versión de la aplicación que se va a instalar. Esta información, junto con el resto de metadatos, está en [PDS](https://pds.srv.vocento.in/).**  

#[[
## ¿Cómo desplegar y configurar los componentes que integran la aplicación?
]]#

Lo común es que una versión funcional de una aplicación esté compuesta de varios componentes como front, back, código sql, procesos batch, ficheros de configuración, etc. Al realizar un despliegue, cada componente deberá instalarse en una/s máquina/s determinada/s, con una configuración específica y con unos requisitos previos (existencia de servicios, directorios, usuarios, ...).

**El encargado de esta tarea será el componente de despliegue y configuración (componente de infraestructura a partir de ahora), se tratará como un componente más de la aplicación '${application}', serán playbooks de `Ansible`, y estarán en este repositorio `Git`.**

![05-componente-infra](https://bitbucket.vocento.com/projects/AR/repos/devops-guides/raw/img/05-componente-infra.png?at=refs%2Fheads%2Fmaster)

El tratar el componente de infraestructura como un elemento más de la aplicación nos permitirá aplicar **distintas configuraciones** y, lo que es más importante, **versionarlas.**  

#[[
## Quiero desplegar la versión 2.4.0-B23 de la aplicación, ¿cómo identificar su componente de infraestructura?
]]#

Una vez estén censados los componentes de una aplicación en **[PDS](https://pds.srv.vocento.in/)**, podremos obtener la URL del componente de infraestructura de la siguiente forma:

```
$ curl https://pds.srv.vocento.in/api/get-artifacts-versions/${application}/2.4.0-B23/infra/
https://nexus.srv.vocento.in/repository/maven-releases/vocento/ansible/${application}/1.3.0-B20/${application}-1.3.0-B20.zip
```

**Aquí tendremos los playbooks con la configuración, roles y scripts de despliegue necesarios para poder instalar la versión 2.4.0-B23 de la aplicación en cada uno de los entornos.**

*Las versión de la aplicación que deseemos desplegar no corresponde normalmente con la versión de los componentes que la integran. Es lo que ocurre en el ejemplo.*
 
#[[
## Tres parámetros: aplicación, versión y entorno de destino
]]#

Esto es todo lo que necesitamos para poder desplegar cualquier versión de una aplicación (con todos los componentes que la integran) en un entorno (des, pre o pro).

Se ha habilitado un **[job en Jenkins](https://jenkins2.srv.vocento.in/job/deploy-application/)**, recibe tres parámetros y se encarga de invocar al shell-script `deploy-application.sh` que descarga y ejecuta el componente de infraestructura que corresponde a la versión de la aplicación a desplegar.   

Ejemplo:
```
$ /jenkins2/workspace/devops-utilities/deploy-application.sh -a ${application} -v 2.4.0-B23 -e des
```

#[[
## Flowchart de deploy-application.sh
]]#

![06-deploy-application](https://bitbucket.vocento.com/projects/AR/repos/devops-guides/raw/img/06-deploy-application.jpg?at=refs%2Fheads%2Fmaster)

#[[
## Playbook app.yml
]]#

Este playbook se va a ejecutar en la máquina de control de Ansible y será invocado por el shell-script `deploy-application.sh` después de que se haya descargado y descomprimido el componente de infraestructura. Antes de su ejecución, se define la variable de entorno `ANSIBLE_CONFIG`:

```
export ANSIBLE_CONFIG=${HOME}/ansible/ansible.cfg
```

Si estamos desplegando la versión `2.4.0-B23` de la aplicación `${application}` en el entorno `des` se invocará de la siguiente forma:

```
ansible-playbook app.yml -i ${HOME}/ansible/inventory.py -e application=${application} -e version=2.4.0-B23 -e entorno=des
```

**El playbook se compone de tres partes:**

1. Obtención de las URLs de los distintos componentes
2. Aplicación del rol correspondiente para cada componente
3. Actualización del nivel de madurez de la versión de la aplicación desplegada

#[[
### Obtención de las URLs de los distintos componentes
]]#

Para conocer los distintos componentes que componen la versión a desplegar, invocamos al siguiente endpoint de **[PDS](https://pds.srv.vocento.in/)**.

```
$ curl -s https://pds.srv.vocento.in/api/get-artifacts-versions/${application}/2.4.0-B23/
{
  "vocento.ansible:${application}": {
    "version": "1.3.0-B20",
    "url": "https://nexus.srv.vocento.in/repository/maven-releases/vocento/ansible/${application}/1.3.0-B20/${application}-1.3.0-B20.zip"
  },
  "vocento.${technology-type}:${artifact-id}": {
    "version": "2.4.0-B2",
    "url": "https://nexus.srv.vocento.in/repository/maven-releases/vocento/${technology-type}/${artifact-id}/2.4.0-B2/${artifact-id}-2.4.0-B2.zip"
  }
}
```

El primer *play* se ejecuta en localhost y registra este diccionario en el fact `artifacts` para que pueda ser accedido desde los distintos roles. Por ejemplo, si las coordenadas de uno de los artefactos es vocento.${technology-type}:${artifact-id}, podremos acceder a la url de este componente de la forma:

`"{{ hostvars['localhost']['artifacts'].json['vocento.${technology-type}:${artifact-id}']['url'] }}"`

y a la versión de la siguiente forma:

`"{{ hostvars['localhost']['artifacts'].json['vocento.${technology-type}:${artifact-id}']['version'] }}"`



#[[
### Aplicación del rol correspondiente para cada componente
]]#

Los siguientes *plays* se encargarán de aplicar los roles de cada artefacto para instalarlo y configurarlo en las máquinas de destino. **El entorno de destino es lo que determinará en qué máquinas se instalará el artefacto y qué configuración se aplicará**.

```  
- hosts: "{{ entorno }}:&${technology-type}:&${application}"
  roles:
    - { role: deploy-artifact-${artifact-id} }
```

#[[
### Actualización del nivel de madurez de la versión de la aplicación desplegada
]]#

Este *play* se encargará de informar a **[PDS](https://pds.srv.vocento.in/)** de que ha finalizado la instalación en el entorno correspondiente para actualizar el estado de la versión de la aplicación.

#[[
## Clonado
]]#

```
$ git clone ${repo-git}
$ cd ${application}
$ git checkout develop
```

**A partir de aquí, crear una rama de nombre `feature/JIRA-ID` para hacer los cambios necesarios.**
