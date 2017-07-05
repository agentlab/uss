# USS

## Features!
  - Поддержка Docker

## API
##### Сервис настроек

###### Получить файл с настройками  
 - Метод: GET 
 - url: /api/blob/{token}/{filename}
 - Формат данных: Json { "username":username, "password":password }
 - Ответ: 401 - UNAUTHORIZED. 404 - NOT_FOUND. 304 - NOT_MODIFIED.  200 - OK + Etag + Json {"value":stream}.

###### Обновить файл с настройками  
 - Метод: PUT 
 - url: /api/blob/{token}/{filename}
 - Формат данных: Json { "value":stream }
 - Ответ: 401 - UNAUTHORIZED. 404 - NOT_FOUND. 409 - CONFLICT. 200 - OK. 201 - CREATED

###### Удалить файл с настройками  
 - Метод: DELETE 
 - url: /api/blob/{token}/{filename}
 - Ответ: 401 - UNAUTHORIZED. 404 - NOT_FOUND. 204 - NO_CONTENT. 494 - REQUEST_HEADER_TO_LARGE
 
##### Сервис пользователей 
###### логин в системе 
 - метод: POST
 - url: /api/user/login
 - Формат данных: Json { "username":username, "password":password }
 - Ответ: 401 - UNAUTHORIZED. 200 - OK + Сессия
    
###### Создание пользоваеля в системе
 - метод: POST
 - url: /api/user/create 
 - Формат данных: Json { "username":username, "password":password, "confirmPassword":password }
 - Ответ: 401 - UNAUTHORIZED. 403 - FORBIDDEN. 200 - OK + Сессия

## Создание Docker image

```sh
$ cd docker/scripts
$ ./assembly.sh [options] 
```
 или 
```sh
$ cd docker/scripts
$ ./assembly_bin_in_docker.sh
$ ./assembly.sh -bd [path to zip with bin]
```

##### Опсиание скрипта ./assembly.sh
 - Запуск ./assembly.sh без параметров приведет к стандартной сборке проекта - mvn clean package. 
   Затем последует сборка Docker image со стандартным именем - uss
 - Запуск скрипта с параметром -n|--container-name предоставит возвожность задать имя Docker image
 - Запуск скрипта с параметром -bd|--bin-dir предоставит возвожность указать расположение zip-архива с    бинарником. Фаза mvn clean package в этом случае будет пропущена
 - Запуск скрипта с параметром --h|--help - справка по опциям

##### Опсиание скрипта ./assembly_bin_in_docker.sh
 - Произведется сборка бинарников в Docker контейнере, который после сборки автоматический удалится. Скрипт отмапит папку юзера .m2 в контейнер и расположит бинарники в стандартных директориях на хосте


