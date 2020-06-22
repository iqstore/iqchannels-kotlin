IQChannels Android SDK
==================
SDK для Андроида сделано как обычная андроид-библиотека, которую можно поставить с помощью Gradle.
Библиотека легко интегрируется как обычная зависимость в существующее приложение.

Требования:
* minSdkVersion 14.
* targetSdkVersion 26.


Установка
---------
Добавить репозиторий с библиотекой в `build.gradle` всего проекта в раздел `allProjects`. 
Это временное требование, в ближайшем будущем библиотека появится в центральном репозитории jCenter.

```build.gradle
allprojects {
    repositories {
        jcenter()
        maven {
            url "https://dl.bintray.com/iqstore/maven/"
        }
    }
}
```

Добавить зависимосить `compile 'ru.iqstore:iqchannels-sdk:1.5.0'` в `build.gradle` модуля приложнеия.
```build.gradle
dependencies {
    compile fileTree(dir: 'libs', include: ['*.jar'])   
    compile 'ru.iqstore:iqchannels-sdk:1.5.0'
    // etc...
}
```

Собрать проект, `gradle` должен успешно скачать все зависимости. 


Конфигурация
------------
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

Анонимный режим
---------------
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

Логин
-----
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


Интерфейс чата
--------------
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


Отображение непрочитанных сообщений
-----------------------------------
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

Настройка пуш-уведомлений
-------------------------
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
