# mq-component-java
Java component for MoniQue.

## Сборка и установка

библиотеку с компонентом можно собрать в исполняемый jar файл с помощью следующих команд:

```bash
git clone https://github.com/biocad/mq-component-java.git
cd mq-component-java
mvn clean package
```

## Использование в сторонних библиотеках

После установки для импорта будут доступны следующие модули:
1. `component`: класс `Config`, `Constant`, `Converter` -запаковкa/распаковкa в `MessagePack`, `MessageUtils` - функции для работы с тегом. Все компоненты должны наследоваться от `MoniqueComponent`
и реализовывать следующие методы - `run` (непосредственно логика компонента), `availableSpecifications` (спецификации сообщений) и `createConfiguration (загрузка конфигурации).    
2. `protocol`: Классы `MoniqueError`, `MoniqueMessage`, `MoniqueTaggedMessage`. 

## Создание нового компонента

Порядок создания нового компонента описан [тут](doc/Develop.md)

## Запуск тестов

Для запуска тестов необходим запущенный scheduler – "одно место". Мы считаем, что "одно место" у вас уже запущено на локальной машине (подробнее читай в документации [mq](https://github.com/biocad/mq)).

Для взаимодействия с тестами можно воспользоваться [jobcontrol](https://github.com/biocad/mq-jobcontrol).

В данный момент для тестирования доступен один класс `SimpleDataTest`. Логика данного теста состоит в следующем: компонент создает простое сообщение и отправляет его в MoniQue в одном потоке, а в другом потоке ожидает это сообщение обратно из MoniQue. 

## Порядок реализации компонента

  * Протокол – описание формата общения с MQ.
    * [Типы](src/main/java/protocol/MoniqueMessage.java). Содержит описание сообщения. 
    * [Ошибки](src/main/java/protocol/ErrorCodes.java). Содержит класс ошибки и коды ошибок.
  * Компонент – общение с MQ.
    * [Компонент](src/main/java/component/MoniqueComponent.java). Содержит класс Компонент, позволяющий слушать и отправлять сообщения, а также содержит коммуникационный, технический каналы и канал ошибок.
     * [Работа с MP](src/main/java/component/Converter.java). Содержит функции для работы с сообщениями: создание, конвертация в/из MessagePack и т.д. 
     * [Тэг](src/main/java/component/TagUtils.java). Содержит функции для работы с тэгом: создание, обращение к полям.