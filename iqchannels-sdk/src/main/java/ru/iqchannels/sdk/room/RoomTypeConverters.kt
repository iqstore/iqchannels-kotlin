package ru.iqchannels.sdk.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.iqchannels.sdk.schema.*
import java.io.File

class SingleChoiceListConverter {
    @TypeConverter
    fun fromList(list: List<SingleChoice>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toList(data: String?): List<SingleChoice>? {
        val type = object : TypeToken<List<SingleChoice>>() {}.type
        return Gson().fromJson(data, type)
    }
}

class ActionListConverter {
    @TypeConverter
    fun fromList(list: List<Action>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun toList(data: String?): List<Action>? {
        val type = object : TypeToken<List<Action>>() {}.type
        return Gson().fromJson(data, type)
    }
}

class FileConverter {
    @TypeConverter
    fun fromFile(file: File?): String? {
        return file?.absolutePath
    }

    @TypeConverter
    fun toFile(path: String?): File? {
        return path?.let { File(it) }
    }
}