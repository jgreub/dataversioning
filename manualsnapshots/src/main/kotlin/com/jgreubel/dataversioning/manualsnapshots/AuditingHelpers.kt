package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.stereotype.Component

@Component
class UserService {

    fun getCurrentUsername(): String {
        return listOf("Billy", "Sally", "Mark", "Annie", "Desmond").random()
    }

}