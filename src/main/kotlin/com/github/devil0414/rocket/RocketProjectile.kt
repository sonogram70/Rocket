package com.github.devil0414.rocket

import com.github.noonmaru.tap.fake.FakeProjectile

open class RocketProjectile(maxTicks: Int, range: Double)  : FakeProjectile(maxTicks, range) {
    lateinit var rocket: Launch
        internal set
}