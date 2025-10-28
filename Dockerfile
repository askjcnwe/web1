# --- СТАДИЯ 1: Сборка ---
FROM eclipse-temurin:17-alpine AS builder

# Копируем библиотеку FastCGI
COPY lib/fastcgi-lib.jar /usr/src/app/lib/fastcgi-lib.jar

# Копируем исходники Java
COPY src/main/java/org/app /usr/src/app/src/org/app

WORKDIR /usr/src/app

# Компилируем все исходники из org/app
RUN javac -cp lib/fastcgi-lib.jar -d . src/org/app/*.java

# --- СТАДИЯ 2: Запуск ---
FROM eclipse-temurin:17-jre-alpine

# Копируем библиотеку и скомпилированные классы
COPY --from=builder /usr/src/app/lib/fastcgi-lib.jar /usr/src/app/lib/fastcgi-lib.jar
COPY --from=builder /usr/src/app/org /usr/src/app/org

WORKDIR /usr/src/app

# Открываем порт FastCGI
EXPOSE 9000

# Запускаем сервер (обрати внимание на пакет org.app)
CMD ["java", "-DFCGI_PORT=9000", "-cp", "lib/fastcgi-lib.jar:.", "org.app.Main"]
