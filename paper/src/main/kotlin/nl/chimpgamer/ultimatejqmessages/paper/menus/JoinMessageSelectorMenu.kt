package nl.chimpgamer.ultimatejqmessages.paper.menus

import io.github.rysefoxx.inventory.plugin.content.IntelligentItem
import io.github.rysefoxx.inventory.plugin.content.InventoryContents
import io.github.rysefoxx.inventory.plugin.content.InventoryProvider
import io.github.rysefoxx.inventory.plugin.events.RyseInventoryOpenEvent
import io.github.rysefoxx.inventory.plugin.other.EventCreator
import io.github.rysefoxx.inventory.plugin.pagination.RyseInventory
import io.github.rysefoxx.inventory.plugin.pagination.SlotIterator
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import nl.chimpgamer.ultimatejqmessages.paper.UltimateJQMessagesPlugin
import nl.chimpgamer.ultimatejqmessages.paper.extensions.*
import nl.chimpgamer.ultimatejqmessages.paper.extensions.getDisplayNamePlaceholder
import nl.chimpgamer.ultimatejqmessages.paper.utils.Utils
import org.bukkit.entity.Player

class JoinMessageSelectorMenu(plugin: UltimateJQMessagesPlugin) :
    ConfigurableMenu(plugin, "join_message_selector.yml") {

    private fun buildInventory() {
        inventory = RyseInventory.builder()
            .provider(object : InventoryProvider {
                override fun init(player: Player, contents: InventoryContents) {
                    val pagination = contents.pagination()
                    val currentPage = pagination.page()
                    val nextPage = pagination.next().page()
                    val previousPage = pagination.previous().page()

                    pagination.itemsPerPage = menuSize - 9

                    val usersHandler = plugin.usersHandler
                    val user = usersHandler.getUser(player.uniqueId)
                    if (user == null) {
                        inventory.close(player)
                        return
                    }
                    val tagResolverBuilder = TagResolver.builder()
                        .resolver(Placeholder.parsed("page", currentPage.toString()))
                        .resolver(Placeholder.parsed("next_page", nextPage.toString()))
                        .resolver(Placeholder.parsed("previous_page", previousPage.toString()))
                        .resolver(Placeholder.parsed("custom_join_message", user.customJoinMessage ?: ""))
                        .resolver(getDisplayNamePlaceholder(player))

                    val joinQuitMessagesHandler = plugin.joinQuitMessagesHandler

                    val lockedJoinMessageItem = menuItems["LockedJoinMessageItem"]
                    val unlockedJoinMessageItem = menuItems["UnlockedJoinMessageItem"]
                    val selectedJoinMessageItem = menuItems["SelectedJoinMessageItem"]

                    joinQuitMessagesHandler.getJoinMessages().forEach { joinMessage ->
                        val selected = user.joinMessage == joinMessage
                        val hasPermission = joinMessage.hasPermission(player)

                        val itemStack = if (!hasPermission) {
                            lockedJoinMessageItem?.itemStack
                        } else if (selected) {
                            selectedJoinMessageItem?.itemStack
                        } else {
                            unlockedJoinMessageItem?.itemStack
                        }
                        if (itemStack == null) return@forEach
                        tagResolverBuilder
                            .resolver(Placeholder.parsed("name", joinMessage.name))
                            .resolver(Placeholder.parsed("join_message_name", joinMessage.name))
                            .resolver(Placeholder.parsed("join_message", joinMessage.message))
                        val tagResolver = tagResolverBuilder.build()

                        val joinQuitMessageSelectItem = updateDisplayNameAndLore(itemStack, player, tagResolver)

                        pagination.addItem(IntelligentItem.of(joinQuitMessageSelectItem) {
                            if (!selected && hasPermission) {
                                user.joinMessage(joinMessage)
                                player.sendMessage(plugin.messagesConfig.joinMessageSet.parse(tagResolver))
                                contents.reload()
                            }
                        })
                    }

                    pagination.iterator(
                        SlotIterator.builder().startPosition(0).type(SlotIterator.SlotIteratorType.HORIZONTAL).build()
                    )

                    val tagResolver = tagResolverBuilder.build()
                    if (!pagination.isFirst) {
                        val previousPageItem = menuItems["PreviousPageItem"]?.itemStack
                        if (previousPageItem != null) {
                            contents[menuSize - 9] =
                                IntelligentItem.of(updateDisplayNameAndLore(previousPageItem, player, tagResolver)) {
                                    inventory.open(player, pagination.previous().page())
                                }
                        }
                    }

                    val customJoinMessageItem = menuItems["CustomJoinMessageItem"]?.itemStack
                    if (customJoinMessageItem != null) {
                        contents[menuSize - 7] =
                            IntelligentItem.of(updateDisplayNameAndLore(customJoinMessageItem, player, tagResolver)) {
                                inventory.close(player)
                                if (!player.hasPermission("ultimatejqmessages.customjoinmessage")) {
                                    player.sendRichMessage(plugin.messagesConfig.noPermission)
                                    return@of
                                }
                                player.sendRichMessage(plugin.messagesConfig.joinMessageCreateCustomChat)

                                val playerInputBuilder = Utils.createChatInputBuilderBase(plugin, player)
                                    .isValidInput { _, input ->
                                        var valid = false

                                        if (input.contains("<displayname>", ignoreCase = true)) {
                                            val component = input.parseOrNull()
                                            if (component != null) {
                                                val maxLength = plugin.settingsConfig.joinMessagesCustomMaxLength
                                                val componentLength = component.length()
                                                if (componentLength > maxLength) {
                                                    player.sendRichMessage(plugin.messagesConfig.joinMessagesCreateCustomTooLong)
                                                } else {
                                                    valid = true
                                                }
                                            }
                                        }
                                        valid
                                    }
                                    .onInvalidInput { player, input ->
                                        player.sendMessage(plugin.messagesConfig.joinMessageCreateInvalidInput.parse(Placeholder.parsed("input", input)))
                                        false
                                    }
                                    .onFinish { player, input ->
                                        player.sendActionBar(Component.empty())

                                        user.customJoinMessage(input)
                                        val title = plugin.messagesConfig.joinMessageCreateCustomSetTitle.toTitle()
                                        player.showTitle(title)
                                        player.sendMessage(plugin.messagesConfig.joinMessageCreateCustomSetChat.parse(
                                            TagResolver.resolver(Placeholder.parsed("custom_join_message", user.customJoinMessage ?: ""), getDisplayNamePlaceholder(player))
                                        ))
                                    }

                                val playerInput = playerInputBuilder.build()
                                playerInput.start()
                                val title = plugin.messagesConfig.joinMessageCreateCustomTitle.toTitle(1L, 300L, 1L)
                                player.showTitle(title)
                            }
                    }

                    val closeMenuItem = menuItems["CloseMenuItem"]?.itemStack
                    if (closeMenuItem != null) {
                        contents[menuSize - 5] =
                            IntelligentItem.of(updateDisplayNameAndLore(closeMenuItem, player, tagResolver)) {
                                inventory.close(player)
                            }
                    }

                    val clearJoinMessageItem = menuItems["ClearJoinMessageItem"]?.itemStack
                    if (clearJoinMessageItem != null) {
                        contents[menuSize - 3] =
                            IntelligentItem.of(updateDisplayNameAndLore(clearJoinMessageItem, player, tagResolver)) {
                                user.clearJoinMessages()
                                player.sendRichMessage(plugin.messagesConfig.joinMessageReset)
                                contents.reload()
                            }
                    }

                    if (!pagination.isLast) {
                        val nextPageItem = menuItems["NextPageItem"]?.itemStack
                        if (nextPageItem != null) {
                            contents[menuSize - 1] =
                                IntelligentItem.of(updateDisplayNameAndLore(nextPageItem, player, tagResolver)) {
                                    inventory.open(player, pagination.next().page())
                                }
                        }
                    }
                }

                override fun close(player: Player, inventory: RyseInventory) {
                    closingSound?.play(player)
                }
            })
            .listener(EventCreator(RyseInventoryOpenEvent::class.java) {
                plugin.inventoryManager.getContents(it.player.uniqueId).ifPresent { contents ->
                    val pagination = contents.pagination()
                    val contentPlaceholders = mapOf(
                        "page" to pagination.page(),
                        "maxpage" to pagination.lastPage()
                    )
                    contents.updateTitle(menuTitle.toString().parse(contentPlaceholders))
                }
            })
            .disableUpdateTask()
            .title(menuTitle.toString().parse())
            .size(menuSize)
            .build(plugin)
    }

    init {
        buildInventory()
    }
}