package ru.iqchannels.sdk.schema

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class Language {
    var code: String = "ru"
    @SerializedName("signup_title")
    var signupTitle: String = "Представьтесь, пожалуйста,"
    @SerializedName("signup_subtitle")
    var signupSubtitle: String = "желательно указать фамилию и имя:"
    @SerializedName("signup_name_placeholder")
    var signupNamePlaceholder: String = "Ваше имя"
    @SerializedName("signup_checkbox_text")
    var signupCheckboxText: String = "Согласие на обработку персональных данных"
    @SerializedName("signup_button_text")
    var signupButtonText: String = "Начать чат"
    @SerializedName("signup_error")
    var signupError: String = "Ошибка: длина имени должна быть не менее 3-х символов."
    @SerializedName("title_error")
    var titleError: String = "Чат временно недоступен"
    @SerializedName("text_error")
    var textError: String = "Мы уже все исправляем. Обновите страницу или попробуйте позже"
    @SerializedName("title_error_pm")
    var titleErrorPm: String = "Нет закреплённого персонального менеджера"
    @SerializedName("text_error_pm")
    var textErrorPm: String = "Обратитесь в чат с тех. поддержкой"
    @SerializedName("button_error")
    var buttonError: String = "Вернуться"
    @SerializedName("status_label")
    var statusLabel: String = "На связи"
    @SerializedName("status_label_awaiting_network")
    var statusLabelAwaitingNetwork: String = "Ожидание сети..."
    @SerializedName("operator_typing")
    var operatorTyping: String = "печатает"
    @SerializedName("input_message_placeholder")
    var inputMessagePlaceholder: String = "Сообщение"
    @SerializedName("text_file_state_rejected")
    var textFileStateRejected: String = "Небезопасный файл"
    @SerializedName("text_file_state_on_checking")
    var textFileStateOnChecking: String = "Файл на проверке"
    @SerializedName("text_file_state_sent_for_check")
    var textFileStateSentForCheck: String = "Файл отправлен на проверку"
    @SerializedName("text_file_state_check_error")
    var textFileStateCheckError: String = "Ошибка проверки файла"
    @SerializedName("rating_state_pending")
    var ratingStatePending: String = "Пожалуйста, оцените качество консультации"
    @SerializedName("rating_state_ignored")
    var ratingStateIgnored: String = "Без оценки оператора"
    @SerializedName("rating_state_rated")
    var ratingStateRated: String = "Оценка оператора {{client_rating}} из {{max_rating}}"
    @SerializedName("new_messages")
    var newMessages: String = "Новые сообщения"
    @SerializedName("sent_rating")
    var sentRating: String = "Отправить"
    @SerializedName("invalid_messsage")
    var invaridMesssage: String = "Неподдерживаемый тип сообщения"
    @SerializedName("image_load_error")
    var imageLoadError: String = "Ошибка загрузки"
    @SerializedName("rating_offer_title")
    var ratingOfferTitle: String = "Желаете пройти опрос?"
    @SerializedName("rating_offer_yes")
    var ratingOfferYes: String = "Да"
    @SerializedName("rating_offer_no")
    var ratingOfferNo: String = "Нет"
    @SerializedName("sender_name_anonym")
    var senderNameAnonym: String = "Аноним"
    @SerializedName("sender_name_system")
    var senderNameSystem: String = "Система"
    @SerializedName("text_copied")
    var textCopied: String = "Сообщение скопировано"
    var copy: String = "Копировать"
    var reply: String = "Ответить"
    var resend: String = "Повторить отправку"
    var delete: String = "Удалить"
    @SerializedName("file_saved_title")
    var fileSavedTitle: String = "Успешно!"
    @SerializedName("file_saved_text")
    var fileSavedText: String = "Файл успешно сохранен."
    @SerializedName("file_saved_error")
    var fileSavedError: String = "Не удалось загрузить файл"
    @SerializedName("photo_saved_success_title")
    var photoSavedSuccessTitle: String = "Успешно!"
    @SerializedName("photo_saved_error_title")
    var photoSavedErrorTitle: String = "Ошибка!"
    @SerializedName("photo_saved_success_text")
    var photoSavedSuccessText: String = "Фото успешно сохранено в галерею."
    @SerializedName("photo_saved_error_text")
    var photoSavedErrorText: String = "Не удалось сохранить фото."
    @SerializedName("gallery_permission_denied_title")
    var galleryPermissionDeniedTitle: String = "Доступ к галерее запрещён"
    @SerializedName("gallery_permission_denied_text")
    var galleryPermissionDeniedText: String = "Пожалуйста, разрешите доступ в настройках, чтобы сохранять фото."
    @SerializedName("gallery_permission_alert_cancel")
    var galleryPermissionAlertCancel: String = "Отмена"
    @SerializedName("gallery_permission_alert_settings")
    var galleryPermissionAlertSettings: String = "В настройки"
    @SerializedName("file_size_error")
    var fileSizeError: String = "Слишком большая ширина или высота изображения"
    @SerializedName("file_weight_error")
    var fileWeightError: String = "Превышен максимально допустимый размер файла"
    @SerializedName("file_not_allowed")
    var fileNotAllowed: String = "Неподдерживаемый тип файла"
    @SerializedName("file_forbidden")
    var fileForbidden: String = "Запрещенный тип файла"
    var gallery: String = "Галерея"
    var file: String = "Файл"
    var camera: String = "Камера"
    var cancel: String = "Отмена"
    var today: String = "Сегодня"

    override fun toString(): String {
        return Gson().toJson(this)
    }
}