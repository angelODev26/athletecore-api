# ========================================
# ETAPA 1: BUILD (Compilación)
# ========================================
FROM eclipse-temurin:21-jdk-alpine AS build

# Variables de entorno para Maven
ENV MAVEN_VERSION=3.9.9
ENV USER_HOME_DIR=/home/appuser
ENV SPRING_PROFILE=docker
ENV DOCKER_BUILDKIT=1

# Directorio de trabajo
WORKDIR /build

# Instalar Maven desde wget (Alpine no tiene apt)
RUN apk add --no-cache curl

# Descargar y configurar Maven
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    | tar -xzf - -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

# Copiar archivos de configuración del proyecto
COPY mvnw .
COPY .mvn .mvn
COPY mvnw.cmd .
COPY pom.xml .

# Descargar dependencias de Maven (capa caché)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar el proyecto (incluye recursos como migraciones Flyway)
RUN ./mvnw clean package -DskipTests -Dmaven.javadoc.skip=true -B

# ========================================
# ETAPA 2: RUNTIME (Ejecución)
# ========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Variables de entorno
ENV JAVA_OPTIONS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker
ENV APP_HOME=/app

# Crear usuario no root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Directorio de trabajo
WORKDIR ${APP_HOME}

# Copiar JAR empaquetado desde la etapa de build
COPY --from=build /build/target/*.jar app.jar

# Establecer propiedad de archivos
RUN chown -R appuser:appgroup ${APP_HOME}

# Cambiar a usuario no root
USER appuser

# Exposición del puerto
EXPOSE 8080

# Healthcheck para Spring Boot
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Comando de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTIONS -jar app.jar"]

# ========================================
# ETAPA 3: DEV (Desarrollo con JLink)
# ========================================
FROM build AS dev

# Crear JRE optimizado (opcional para producción, pero útil para dev)
RUN java -jlink --add-modules ALL-MODULE-PATH \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --output jre \
    --module-path lib \
    --add-modules java.base,java.sql,java.naming,jdk.unsupported

# Sobreescribe el JRE del sistema por el optimizado (opcional)
# Este paso es opcional y puede aumentar el tiempo de build
