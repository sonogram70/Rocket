package com.github.devil0414.rocket

import com.github.noonmaru.tap.fake.FakeEntityServer

class RocketPluginScheduler(val fakeEntityServer: FakeEntityServer) : Runnable {
    override fun run() {

        FileManager.fakeEntityServer.update()
        for(rocket in FileManager.rockets.values) {
            rocket.launch?.update()
        }
        fakeEntityServer.update()
    }
}