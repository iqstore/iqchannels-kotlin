package ru.iqchannels.sdk.http

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException

internal class HttpSseReader(private val reader: BufferedReader) : Closeable {
	@Throws(IOException::class)
	fun readEvent(): HttpSseEvent? {
		// Read the next non-empty event.
		while (true) {
			var eof = true
			var eventId: String? = ""
			var eventName: String? = ""
			val eventBuffer = StringBuilder()

			// Read an event.
			while (true) {
				val line = reader.readLine()
					?: // Once the end of the file is reached, the user agent must dispatch the event one final time, 
// as defined below.
					break
				eof = false

				// If the line is empty (a blank line)
				// Dispatch the event, as defined below.
				if (line.isEmpty()) {
					break
				}

				// If the line starts with a U+003A COLON character (:)
				// Ignore the line.
				if (line.startsWith(":")) {
					// Comment.
					continue
				}
				var name = ""
				var value = ""
				val i = line.indexOf(":")
				if (i >= 0) {
					// If the line contains a U+003A COLON character character (:)
					// Collect the characters on the line before the first U+003A COLON character (:), 
					// and let field be that string.
					// 
					// Collect the characters on the line after the first U+003A COLON character (:), 
					// and let value be that string. If value starts with a single U+0020 SPACE character, 
					// remove it from value.
					// 
					// Process the field using the steps described below, 
					// using field as the field name and value as the field value.
					name = line.substring(0, i)
					value = if (i == line.length - 1) {
						""
					} else {
						line.substring(i + 1, line.length)
					}
					if (!value.isEmpty() && value[0] == ' ') {
						value = value.substring(1)
					}
				} else {
					// Otherwise, the string is not empty but does not contain a U+003A COLON character character (:)
					// Process the field using the steps described below, using the whole line as the field name, 
					// and the empty string as the field value.
					name = line
					value = ""
				}
				when (name) {
					"event" ->                         // If the field name is "event"
						// Set the event name buffer to field value.
						eventName = value

					"data" -> {
						// If the field name is "data"
						// Append the field value to the data buffer, 
						// then append a single U+000A LINE FEED (LF) character to the data buffer.
						eventBuffer.append(value)
						eventBuffer.append('\n')
					}

					"id" ->                         // If the field name is "id"
						// Set the event stream's last event ID to the field value.
						eventId = value

					"retry" ->                         // If the field name is "retry"
						// If the field value consists of only characters in the range U+0030 DIGIT ZERO (0) 
						// to U+0039 DIGIT NINE (9), 
						// then interpret the field value as an integer in base ten, and set the event stream's 
						// reconnection time to that integer. Otherwise, ignore the field.
						continue

					else -> {}
				}
			}
			if (eof) {
				return null
			}

			// When the user agent is required to dispatch the event, then the user agent must act as follows:
			var eventData = eventBuffer.toString()

			// 1. If the data buffer is an empty string, set the data buffer 
			//    and the event name buffer to the empty string and abort these steps.
			if (eventData.isEmpty()) {
				continue
			}

			// 2. If the data buffer's last character is a U+000A LINE FEED (LF) character, 
			//    then remove the last character from the data buffer.
			if (!eventData.isEmpty() && eventData[eventData.length - 1] == '\n') {
				eventData = eventData.substring(0, eventData.length - 1)
			}

			// 3. If the event name buffer is not the empty string but is also not a valid event type name, 
			//    as defined by the DOM Events specification, set the data buffer and the event name buffer 
			//    to the empty string and abort these steps. [DOMEVENTS]
			// 4. Otherwise, create an event that uses the MessageEvent interface, with the event name message, 
			//    which does not bubble, is not cancelable, and has no default action. The data attribute 
			//    must be set to the value of the data buffer, the origin attribute must be set to the Unicode 
			//    serialization of the origin of the event stream's URL, and the lastEventId attribute 
			//    must be set to the last event ID string of the event source.
			// 5. If the event name buffer has a value other than the empty string, change the type of the newly
			//    applyCreatedEvent event to equal the value of the event name buffer.
			// 6. Set the data buffer and the event name buffer to the empty string.
			// 7. Queue a task to dispatch the newly applyCreatedEvent event at the EventSource object.
			return HttpSseEvent(eventId, eventName, eventData)
		}
	}

	@Throws(IOException::class)
	override fun close() {
		reader.close()
	}
}
