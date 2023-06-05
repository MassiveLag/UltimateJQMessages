package nl.chimpgamer.ultimatejqmessages.paper.configurations

import dev.dejvokep.boostedyaml.YamlDocument
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings
import nl.chimpgamer.ultimatejqmessages.paper.UltimateJQMessagesPlugin

class SettingsConfig(plugin: UltimateJQMessagesPlugin) {
    val config: YamlDocument

    val storageType: String get() = config.getString("storage.type", "sqlite")
    val storageHost: String get() = config.getString("storage.host", "localhost")
    val storagePort: Int get() = config.getInt("storage.port", 3306)
    val storageDatabase: String get() = config.getString("storage.database", "ultimatemobcoins")
    val storageUsername: String get() = config.getString("storage.username", "ultimatemobcoins")
    val storagePassword: String get() = config.getString("storage.password", "ultimatemobcoins")
    val storageProperties: Map<String, String> get() = config.getSection("storage.properties").getStringRouteMappedValues(false).mapValues { it.value.toString() }

    val joinMessagesCustomMaxLength: Int get() = config.getInt("join_messages.custom.max_length")
    val joinMessagesCooldown: Long get() = config.getLong("join_messages.cooldown")

    val quitMessagesCustomMaxLength: Int get() = config.getInt("quit_messages.custom.max_length")
    val quitMessagesCooldown: Long get() = config.getLong("quit_messages.cooldown")

    init {
        val file = plugin.dataFolder.resolve("settings.yml")
        val inputStream = plugin.getResource("settings.yml")
        val loaderSettings = LoaderSettings.builder().setAutoUpdate(true).build()
        val updaterSettings = UpdaterSettings.builder().setVersioning(BasicVersioning("config-version")).build()
        config = if (inputStream != null) {
            YamlDocument.create(file, inputStream, GeneralSettings.DEFAULT, loaderSettings, DumperSettings.DEFAULT, updaterSettings)
        } else {
            YamlDocument.create(file, GeneralSettings.DEFAULT, loaderSettings, DumperSettings.DEFAULT, updaterSettings)
        }
    }
}