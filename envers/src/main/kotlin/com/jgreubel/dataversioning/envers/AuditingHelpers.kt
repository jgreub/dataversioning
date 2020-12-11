package com.jgreubel.dataversioning.envers

import org.springframework.stereotype.Component

@Component
class UserService {

    fun getCurrentUsername(): String {
        return listOf("Billy", "Sally", "Mark", "Annie", "Desmond").random()
    }

}