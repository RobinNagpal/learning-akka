package com.akka.home

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}

/**
  * Sets up custom components for Play.
  *
  * https://www.playframework.com/documentation/latest/ScalaDependencyInjection
  */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {

  }
}
