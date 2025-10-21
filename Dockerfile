# ---- Build stage ----
FROM gradle:8.3-jdk17 AS builder
WORKDIR /app

# Копируем Gradle файлы и локальную библиотеку
COPY build.gradle ./
COPY lib ./lib

# Копируем исходники
COPY src ./src

# Собираем jar
RUN gradle clean build 

# ---- Run stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем jar из стадии сборки
COPY --from=builder /app/build/libs/*.jar ./app.jar
COPY lib ./lib

# Устанавливаем порт FastCGI
ENV FCGI_PORT=9000
EXPOSE 9000

# Запуск
CMD ["java", "-DFCGI_PORT=9000", "-cp", "app.jar:lib/*", "org.app.Main"]
