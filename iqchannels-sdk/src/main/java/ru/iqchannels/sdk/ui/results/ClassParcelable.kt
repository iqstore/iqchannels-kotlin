package ru.iqchannels.sdk.ui.results

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ClassParcelable<T> (
	val clazz: Class<T>
) : Parcelable

fun Class<*>.toParcelable() = ClassParcelable(this)