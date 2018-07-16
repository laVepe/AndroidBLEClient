package com.vepe.bleapp.bl


sealed class ConnectionStatus {

    class NotEngaged: ConnectionStatus() {

    }

    class Connecting: ConnectionStatus() {

    }

    class Success: ConnectionStatus() {

    }

    class Error: ConnectionStatus() {

    }
}