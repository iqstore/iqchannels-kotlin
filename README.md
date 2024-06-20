IQChannels Android SDK
==================
SDK для Андроида сделано как обычная андроид-библиотека, которую можно поставить с помощью Gradle.
Библиотека легко интегрируется как обычная зависимость в существующее приложение.

Требования:
* minSdkVersion 21.
* targetSdkVersion 34.


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

4. Добавьте репозиторий `https://maven.pkg.github.com/iqstore/iqchannels-kotlin` в `build.gradle` всего проекта в раздел `allProjects`.
Загрузите `local.properties` и укажите `username` и `password` из них:

```build.gradle
Properties properties = new Properties()
properties.load(project.rootProject.file('local.properties').newDataInputStream())

allprojects {
    repositories {
        jcenter()

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/iqstore/iqchannels-kotlin")

            credentials(PasswordCredentials) {
                username = properties['gpr.user'] ?: System.getenv("GPR_USER")
                password = properties['gpr.key'] ?: System.getenv("GPR_API_KEY")
            }
        }
    }
}
```

5. Добавьте зависимосить `implementation 'ru.iqstore:iqchannels-sdk-2:2.0.0-rc1'` в `build.gradle` модуля приложения.
```build.gradle
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])   
    implementation 'ru.iqstore:iqchannels-sdk-2:2.0.0-rc1'
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
Для конфигурации нужно передать адрес чат-сервера и английское название канала в `IQChannels.configure()`.

Пример конфигурации в `Activity.onCreate`.
```kotlin
class MainActivity : AppCompatActivity(),
         NavigationView.OnNavigationItemSelectedListener {
			 
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        
        setupIQChannels()
    }

    private fun setupIQChannels() {
        // Настраиваем сервер и канал iqchannels.
        IQChannels.configure(this, IQChannelsConfig("http://192.168.31.158:3001/", "support"))
    }
}
```

# Анонимный режим
По умолчанию в анонимном режиме пользователю предлагается представиться, чтобы начать чать.
Для автоматического создания анонимного пользователя нужно вызвать:

```kotlin
IQChannels.loginAnonymous()
```

Анонимный пользователь привязывается к устройству. Если требуется удалить анонимный чат, тогда
нужно вызывать:

```kotlin
IQChannels.logoutAnonymous()
```

# Логин
Если `login()` не вызывать, тогда IQChannels функционирует в анонимном режиме. В этом случае
клиенту предлагается представиться и начать неавторизованный чат.

Если на сервере настроено подключение к CRM или ДБО, тогда можно авторизовать клиента в этих системах.
Логин/логаут пользователя осуществляется по внешнему токену, специфичному для конкретного приложения.
Для логина требуется вызвать в любом месте приложения:

```kotlin
IQChannels.login("isimple chat token")
```

Для логаута:
```kotlin
IQChannels.logout()
```

После логина внутри SDK авторизуется сессия пользователя и начинается бесконечный цикл, который подключается
к серверу и начинает слушать события о новых сообщения, при обрыве соединения или любой другой ошибке
сессия переподключается к серверу. При отсутствии сети, сессия ждет, когда последняя станет доступна.


# Интерфейс чата
Интерфес чата построен как отдельный фрагмент, который можно использовать в любом месте в приложении.
Интерфейс чата корректно обрабатывает логины/логаут, обнуляет сообщения.

Пример использования в стандартном боковом меню:

```kotlin
class MainActivity : AppCompatActivity(),
	NavigationView.OnNavigationItemSelectedListener {

	override fun onNavigationItemSelected(item: MenuItem): Boolean {
		val fragment: Fragment = when (item.getItemId()) {
			R.id.nav_index -> {
				PlusOneFragment.newInstance()
			}
			R.id.nav_chat -> {
				// Пользователь выбрал чат в меню.
				ChatFragment.newInstance()
			}
			else -> {
				PlusOneFragment.newInstance()
			}
		}

		// Заменяем фрагмент в нашей активити.
		val fragmentManager = supportFragmentManager
		fragmentManager.beginTransaction().replace(R.id.content, fragment).commit()

		// Сворачиваем меню.
		val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
		drawer.closeDrawer(GravityCompat.START)

		return true
	}
}
```


# Отображение непрочитанных сообщений
Для отображения и обновления непрочитанных сообщений нужно добавить слушателя в IQChannels.
Слушателя можно добавлять в любой момент времени, он корректно обрабатывает переподключения,
логин, логаут.

Пример добавления слушателя в `Activity.onCreate`:
```kotlin
class MainActivity : AppCompatActivity,
        NavigationView.OnNavigationItemSelectedListener,
                   UnreadListener {

    private var unreadSubscription : Cancellable? = null

    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        listenToUnread()
    }

    // Добавляем слушателя непрочитанных сообщений.
    private fun listenToUnread() {
        unreadSubscription = IQChannels.addUnreadListener(this)
    }

    // Показывает текущие количество непрочитанных сообщений.
    override fun unreadChanged(unread: Int) {

    }
}
```

# Настройка пуш-уведомлений
SDK поддерживает пуш-уведомления о новых сообщениях в чате.
Для этого в приложении настроить Firebase Messaging, получить пуш-токен
и передать его в IQChannels.

Передача токена в `Activity.onCreate`:
```kotlin
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        val token = FirebaseInstanceId.getInstance().getToken()

	    IQChannels.configure(this, IQChannelsConfig("https://chat.example.com/", "support"))
	    IQChannels.setPushToken(token)
    }
```

Для регистрации HMS-токена, передайте флаг true в методе:
```kotlin
    IQChannels.setPushToken(token, isHuawei = true)
```

# Кроссегментация
Для того, чтобы запустить режим с несколькими каналами, нужно вызвать инициализацию через IQChannelsFactory:
```kotlin
    IQChannelsFactory().create(
        context = this,
        config = IQChannelsConfig2(
            address = address,
            channels = channels.toList()
        ),
        credentials = ...
    )

    ChannelsFragment.newInstance(navBarEnabled = false)
```

Также можно открыть конкретный канал после инициализации:
```kotlin
    IQChannelsShortCuts.showChat(
        channel = channelName,
        chatType = ChatType.REGULAR,
        fragmentManager,
        containerId = R.id.content
	)
```

# Пример использования стилизации
Для того чтобы поменять стили элементов внутри SDK, нужно передать поддерживаемый JSON файл при инициализации как в примере ниже:
```kotlin
    val stylesJson = // JSON в формате String
    ChatFragment.newInstance(stylesJson = stylesJson)
```

# Пример JSON для передачи в SDK

```json
{
  "chat": {
    "background": {
      "light": "#FFFFFF",
      "dark": "#FFFFE0"
    },
    "date_text": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 13
    },
    "chat_history": {
      "light": "#008080",
      "dark": "#008080"
    },
    "icon_operator": "https://gas-kvas.com/grafic/uploads/posts/2024-01/gas-kvas-com-p-logotip-cheloveka-na-prozrachnom-fone-4.png",
    "system_text": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    }
  },
  "messages": {
    "background_operator": {
      "light": "#FFFFE0",
      "dark": "#808080"
    },
    "background_client": {
      "light": "#242729",
      "dark": "#808080"
    },
    "text_operator": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_client": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "reply_text_client": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "reply_sender_text_client": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "reply_text_operator": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "reply_sender_text_operator": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_time": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_up": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    }
  },
  "answer": {
    "text_sender": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_message": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "background_text_up_message": {
      "light": "#FFFACD",
      "dark": "#808080"
    },
    "icon_cancel": "https://w7.pngwing.com/pngs/21/486/png-transparent-undo-common-toolbar-icon.png",
    "left_line": {
      "light": "#FF0000",
      "dark": "#FF0000"
    }
  },
  "messages_file": {
    "text_filename_client": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_filename_operator": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "icon_file_client": "https://1000logos.net/wp-content/uploads/2023/01/Google-Docs-logo.png",
    "icon_file_operator": "https://1000logos.net/wp-content/uploads/2023/01/Google-Docs-logo.png",
    "text_file_size_client": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "text_file_size_operator": {
      "color": {
        "light": "#ffffff",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    }
  },
  "rating": {
    "background_container": {
      "light": "#FFFACD",
      "dark": "#808080"
    },
    "full_star": "https://img2.freepng.ru/20180621/itr/kisspng-business-5-star-probot-artistry-hotel-farah-5b2bdea0157717.8623271415296016960879.jpg",
    "empty_star": "https://www.downloadclipart.net/large/rating-star-background-png.png",
    "sent_rating": {
      "color_enabled": {
        "light": "#008080",
        "dark": "#008080"
      },
      "color_disabled": {
        "light": "#B7B7CA",
        "dark": "#B7B7CA"
      },
      "text_enabled": {
        "color": {
          "light": "#ffffff",
          "dark": "#FFFFFF"
        },
        "text_size": 10
      },
      "text_disabled": {
        "color": {
          "light": "#ffffff",
          "dark": "#FFFFFF"
        },
        "text_size": 10
      }
    }
  },
  "tools_to_message": {
    "icon_sent": "https://e7.pngegg.com/pngimages/414/329/png-clipart-computer-icons-share-icon-edit-angle-triangle.png",
    "background_icon": {
      "light": "#DEB887",
      "dark": "#696969"
    },
    "background_chat": {
      "light": "#DEB887",
      "dark": "#696969"
    },
    "text_chat": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "icon_clip": "https://cdn-icons-png.flaticon.com/512/84/84281.png"
  },
  "error": {
    "title_error": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 16
    },
    "text_error": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "icon_error": "https://w7.pngwing.com/pngs/285/84/png-transparent-computer-icons-error-super-8-film-angle-triangle-computer-icons.png"
  },
  "single-choice": {
    "background_button": {
      "light": "#FFFF00",
      "dark": "#00FFFF"
    },
    "border_button": {
      "size": 3,
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "border-radius": 10
    },
    "text_button": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    },
    "background_IVR": {
      "light": "#00000000",
      "dark": "#00000000"
    },
    "border_IVR": {
      "size": 1,
      "color": {
        "light": "#74b928",
        "dark": "#74b928"
      },
      "border-radius": 10
    },
    "text_IVR": {
      "color": {
        "light": "#000000",
        "dark": "#FFFFFF"
      },
      "text_size": 10
    }
  },
  "theme": "light"
}
```