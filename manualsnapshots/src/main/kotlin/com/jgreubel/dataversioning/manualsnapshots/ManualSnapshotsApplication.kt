package com.jgreubel.dataversioning.manualsnapshots

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ManualSnapshotsApplication

fun main(args: Array<String>) {
	runApplication<ManualSnapshotsApplication>(*args)
}
