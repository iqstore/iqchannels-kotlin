package ru.iqchannels.sdk.configs

import ru.iqchannels.sdk.schema.ChatFilesConfig

interface GetConfigsInteractor {

	suspend fun getFileConfigs(): ChatFilesConfig?
}