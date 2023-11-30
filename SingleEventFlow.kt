package com.zoho.people.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SingleEventFlow<Type>(
    private val mode: FlowMode = FlowMode.HoldValuesIfNoSubscribers,
    private val scope: CoroutineScope,
) {
    val flow: Flow<Type>
    private var channel: Channel<Type>? = null
    private var sharedFlow: MutableSharedFlow<Type>? = null

    init {
        flow = when (mode) {
            FlowMode.HoldValuesIfNoSubscribers -> {
                channel = Channel(Channel.BUFFERED)
                channel!!.receiveAsFlow()
            }
            FlowMode.IgnoreValuesIfNoSubscribers -> {
                sharedFlow = MutableSharedFlow()
                sharedFlow!!
            }
        }
    }
    fun trySend(event: Type) {
        when (mode) {
            FlowMode.HoldValuesIfNoSubscribers -> {
                channel!!.trySend(event)
            }
            FlowMode.IgnoreValuesIfNoSubscribers -> {
                scope.launch {
                    // To fix https://github.com/Kotlin/kotlinx.coroutines/issues/2387
                    sharedFlow!!.emit(event)
                }
            }
        }
    }
}

enum class FlowMode {
    HoldValuesIfNoSubscribers,
    IgnoreValuesIfNoSubscribers
}

/*
  HoldValuesIfNoSubscribers - Say you are posting an even in ViewModel init block
  and you are observing it in a composable. If you use this mode, the event will be
  collected in Composable block even if it is posted before the composable is created.

  Ex - Compose creation takes more time due to onPause call but the message "Permission denied" is sent
  from ViewModel init block. If you use this mode, the message will be collected in Composable block
  even if it is posted before the composable is created.
  The communication somewhat works even if subscriber was inactive (not added)


  IgnoreValuesIfNoSubscribers - Say you want to refresh the feed whenever a new comment is added in the feed
  New comment added event will be posted in PeopleMessageService
  The event will be collected in FeedDetailsFragment onViewCreated block
  Say the user is in Dashboard and have not went to FeedDetailsFragment, the event will be ignored
  There won't be any subscribers, the event won't be stored at all
  Basically the communication works only if both the publisher and subscriber are active

  Read the below code for better understanding, it somewhat works like this
  var messages = List<String>()
  var subscribers = List<Subscriber>()

  fun sendEvent(message) {
    messages.add(message)
    if (mode == HoldValuesIfNoSubscribers) {
        if (subscribers.isEmpty()) {
            ------- Messages are will be stored and given to subscribers --------
            return
        }
    }

    subscribers.forEach { subscriber ->
       messages.forEach {
          subscriber(messages)
       }
    }
    messages = emptyList()
  }

  fun onSubscribed(subscriber: Subscriber) {
    if (mode == HoldValuesIfNoSubscribers) {
       messages.forEach {
         subscriber(it)
        }
        messages = emptyList()
    }
    subscribers.add(subscriber)
  }

 */
