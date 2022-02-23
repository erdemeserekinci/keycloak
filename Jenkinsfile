node {
    checkout scm

    stage('Package .jar') {
        withMaven(maven: 'mvn') {
            sh "mvn clean install -DskipTests -P distribution"
            sh "cp distribution/server-dist/src/main/modules/layers.conf distribution/server-dist/target/keycloak-11.0.0-SNAPSHOT/modules/layers.conf"
        }
    }
    stage('Build docker image') {
        docker.build("erdemeserekinci/keycloak/keycloak:11.0.42g", './distribution/server-dist')
    }
    stage('Push image to registry') {
        docker.withRegistry('https://docker.pkg.github.com', 'github-bot') {
            def snapshotImage = docker.image("erdemeserekinci/keycloak/keycloak:11.0.42g")
            snapshotImage.push()
            snapshotImage.push('latest')
        }
    }
}
