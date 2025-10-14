FROM openjdk:17-jdk-slim

WORKDIR /app

# Копируем исходники и зависимости
COPY src ./src
COPY lib ./lib

# Компиляция и упаковка JAR
RUN mkdir -p out && \
    javac -cp "lib/*" -d out $(find src -name "*.java") && \
    jar cf app.jar -C out .

# Устанавливаем порт FastCGI
ENV FCGI_PORT=9000
EXPOSE 9000

# Запуск (обязательно: -DFCGI_PORT=9000)
CMD ["java", "-DFCGI_PORT=9000", "-cp", "app.jar:lib/*", "org.app.Main"]
