package com.github.devil0414.rocket

class RocketPluginScheduler : Runnable {
    override fun run() {

        FileManager.fakeEntityServer.update()
        for(rocket in FileManager.rockets.values) {
            rocket.launch?.update()
        }
    }
}