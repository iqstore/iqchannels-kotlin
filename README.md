IQChannels Android SDK
==================
SDK для Андроида сделано как обычная андроид-библиотека, которую можно поставить с помощью Gradle.
Библиотека легко интегрируется как обычная зависимость в существующее приложение.

Требования:
* minSdkVersion 26.
* targetSdkVersion 30.


# Установка
IQChannels SDK использует Maven репозиторий Github Packages. Для установки:

1. Сгенерируйте токен доступа к Github Packages с правами `read:packages` на странице https://github.com/settings/tokens/

2. Сохраните имя пользователя и токен доступа в файле `local.properties`:
```
gpr.user=<github_username>
gpr.key=<ghp_token>
```

3. Добавьте файл `local.properties` в `.gitignore`:
```
local.properties
```

4. Добавьте репозиторий `https://maven.pkg.github.com/iqstore/iqchannels-android` в `build.gradle` всего проекта в раздел `allProjects`.
Загрузите `local.properties` и укажите `username` и `password` из них:

```build.gradle
Properties properties = new Properties()
properties.load(project.rootProject.file('local.properties').newDataInputStream())

allprojects {
    repositories {
        jcenter()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/iqstore/iqchannels-android")

            credentials(PasswordCredentials) {
                username = properties['gpr.user'] ?: System.getenv("GPR_USER")
                password = properties['gpr.key'] ?: System.getenv("GPR_API_KEY")
            }
        }
    }
}
```

5. Добавьте зависимосить `compile 'ru.iqstore:iqchannels-sdk:2.0.0'` в `build.gradle` модуля приложения.
```build.gradle
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])   
    compile 'ru.iqstore:iqchannels-sdk:2.0.0'
    // etc...
}
```

6. Соберите проект, `gradle` должен успешно скачать все зависимости.

7. Также подключите репозиторий в build.gradle на уровне проекта
```build.gradle
allprojects {
    repositories {
        maven { url "https://www.jitpack.io" }
    }
}

buildscript {
    repositories {
        maven { url "https://www.jitpack.io" }
    }	
}
```

Подробнее смотрите в официальной документации Github https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry.


# Конфигурация
Приложение разделено на два основных класса: `IQChannels` и `ChatFragment`. Первый представляет собой библиотеку, 
которая реализуюет бизнес-логику. Второй - это фрагмент, который реализует пользовательский интерфейс чата.

Для использования SDK его нужно сконфигурировать. Конфигурировать можно в любом месте приложения, 
где доступен контекст андроида. `ChatFragment` можно спокойно использовать до конфигурации.
Для конфигурации нужно передать адрес чат-сервера и английское название канала в `IQChannels.instance().configure()`.

Пример конфигурации в `Activity.onCreate`.
```java
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setupIQChannels();
    }

    private void setupIQChannels() {
        // Настраиваем сервер и канал iqchannels.
        IQChannels iqchannels = IQChannels.instance();
        iqchannels.configure(this, new IQChannelsConfig("http://192.168.31.158:3001/", "support"));
    }
}
```

# Анонимный режим
По умолчанию в анонимном режиме пользователю предлагается представиться, чтобы начать чать.
Для автоматического создания анонимного пользователя нужно вызвать:

```java
IQChannels.instance().loginAnonymous();
```

Анонимный пользователь привязывается к устройству. Если требуется удалить анонимный чат, тогда
нужно вызывать:

```java
IQChannels.instance().logoutAnonymous();
```

# Логин
Если `login()` не вызывать, тогда IQChannels функционирует в анонимном режиме. В этом случае
клиенту предлагается представиться и начать неавторизованный чат.

Если на сервере настроено подключение к CRM или ДБО, тогда можно авторизовать клиента в этих системах.
Логин/логаут пользователя осуществляется по внешнему токену, специфичному для конкретного приложения.
Для логина требуется вызвать в любом месте приложения:

```java
IQChannels.instance().login("isimple chat token");
```

Для логаута:
```java
IQChannels.instance().logout();
```

После логина внутри SDK авторизуется сессия пользователя и начинается бесконечный цикл, который подключается
к серверу и начинает слушать события о новых сообщения, при обрыве соединения или любой другой ошибке
сессия переподключается к серверу. При отсутствии сети, сессия ждет, когда последняя станет доступна.


# Интерфейс чата
Интерфес чата построен как отдельный фрагмент, который можно использовать в любом месте в приложении.
Интерфейс чата корректно обрабатывает логины/логаут, обнуляет сообщения.

Пример использования в стандартном боковом меню:
```java
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Fragment fragment;
        switch (item.getItemId()) {
            case R.id.nav_index:
                fragment = PlusOneFragment.newInstance();
                break;

            case R.id.nav_chat:
                // Пользователь выбрал чат в меню.
                fragment = ChatFragment.newInstance();
                break;

            default:
                fragment = PlusOneFragment.newInstance();
                break;
        }

        // Заменяем фрагмент в нашей активити.
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content, fragment).commit();

        // Сворачиваем меню.
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
```


# Отображение непрочитанных сообщений
Для отображения и обновления непрочитанных сообщений нужно добавить слушателя в IQChannels.
Слушателя можно добавлять в любой момент времени, он корректно обрабатывает переподключения,
логин, логаут.

Пример добавления слушателя в `Activity.onCreate`:
```java
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                   UnreadListener {

    private Cancellable unreadSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        listenToUnread();
    }

    // Добавляем слушателя непрочитанных сообщений.
    private void listenToUnread() {
        unreadSubscription = IQChannels.instance().addUnreadListener(this);
    }

    // Показывает текущие количество непрочитанных сообщений.
    @Override
    public void unreadChanged(int unread) {

    }
}
```

# Настройка пуш-уведомлений
SDK поддерживает пуш-уведомления о новых сообщениях в чате.
Для этого в приложении настроить Firebase Messaging, получить пуш-токен
и передать его в IQChannels.

Передача токена в `Activity.onCreate`:
```
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = FirebaseInstanceId.getInstance().getToken();

        IQChannels iq = IQChannels.instance();
        iq.configure(this, new IQChannelsConfig("https://chat.example.com/", "support"));
        iq.setPushToken(token);
    }
```

Обновление токена в наследнике `FirebaseInstanceIdService`:
```
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String token = FirebaseInstanceId.getInstance().getToken();
        IQChannels.instance().setPushToken(token);
    }
}
```
