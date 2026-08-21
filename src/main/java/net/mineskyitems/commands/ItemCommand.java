package net.mineskyitems.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.mineskyitems.MineSkyItems;
import net.mineskyitems.entities.item.ObtainingMethod;
import net.mineskyitems.events.DummyEvent;
import net.mineskyitems.gui.crafting.CraftingCreatorGUI;
import net.mineskyitems.gui.crafting.CraftingManager;
import net.mineskyitems.gui.editor.ItemBuilderMenu;
import net.mineskyitems.entities.item.Item;
import net.mineskyitems.entities.ItemBuilder;
import net.mineskyitems.entities.item.ItemHandler;
import net.mineskyitems.entities.categories.Category;
import net.mineskyitems.entities.categories.CategoryHandler;
import net.mineskyitems.gui.blacksmith.ItemRecyclerMenu;
import net.mineskyitems.gui.blacksmith.ItemRepairMenu;
import net.mineskyitems.gui.rotatingshop.RotatingItemsGUI;
import net.mineskyitems.gui.rotatingshop.armors.RotatingArmorsGUI;
import net.mineskyitems.gui.smelting.SmeltingCreatorGUI;
import net.mineskyitems.gui.smelting.SmeltingManager;
import net.mineskyitems.gui.tinkering.TinkeringCreatorGUI;
import net.mineskyitems.gui.tinkering.TinkeringGUI;
import net.mineskyitems.gui.tinkering.TinkeringQueueCreator;
import net.mineskyitems.gui.tinkering.recipe.TinkeringManager;
import net.mineskyitems.gui.tinkering.recipe.TinkeringRecipe;
import net.mineskyitems.scripts.ArmorStandScript;
import net.mineskyitems.scripts.ItemFrameGenerator;
import net.mineskyitems.utils.Utils;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemCommand implements TabExecutor {

    public static final List<String> subCommands = Arrays.asList("criar", "force-add", "criar-tinkerer", "crafting", "smelting", "tinkering", "contar", "script", "get-all", "category", "editar", "give", "get", "reload", "achar", "deletar", "danificar", "menu");
    public static final List<String> menu_subCommands = Arrays.asList("reparar", "destruir", "shop", "tinkering");
    public static final List<String> craftingtinkering_subCommands = Arrays.asList("create", "delete", "reload");
    public static final List<String> scripts = Arrays.asList("empty", "category", "register-obtainings", "convert", "single", "armor");

    void commandList(CommandSender s) {
        s.sendMessage(Utils.PURPLE_COLOR+Utils.c("&lMineSkyItems v"+MineSkyItems.getInstance().getDescription().getVersion()));
        s.sendMessage(Utils.c(
                Utils.PURPLE_COLOR+"/item criar <categoria> &8- &7Cria um novo item\n"+
                        Utils.PURPLE_COLOR+"/item editar <nome> &8- &7Edita um item já criado a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item give <player> <nome> &8- &7Pega uma cópia do item a partir do nome para um jogador\n"+
                        Utils.PURPLE_COLOR+"/item deletar <nome> &8- &7Deleta um item existente\n"+
                        Utils.PURPLE_COLOR+"/item get <nome> &8- &7Pega uma cópia do item a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item get-all <categoria> &8- &7Pega uma cópia de todos os itens de uma categoria\n"+
                        Utils.PURPLE_COLOR+"/item category <categoria> &8- &7Lista a categoria com seus devidos itens\n"+
                        Utils.PURPLE_COLOR+"/item get <nome> &8- &7Pega uma cópia do item a partir do nome\n"+
                        Utils.PURPLE_COLOR+"/item menu <menu> &8- &7Abre um menu de item, ex: menu de destruir itens para virar pó\n"+
                        Utils.PURPLE_COLOR+"/item reload &8- &7Recarregar o plugin (não recomendado)\n"+
                        Utils.PURPLE_COLOR+"/item danificar <dano> &8- &7Danifica o item de sua mão na quantidade informada\n"+
                        Utils.PURPLE_COLOR+"/item achar [id, nome ou nada] &8- &7Procura um item pela parte do nome dele, ou pelo seu ID, ou pelo item em sua mão."
        ));
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 0.5f, 1);
        }

        if(args.length == 0) {
            if(!s.hasPermission("mineskyitems.command.help"))
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            else commandList(s);
            return true;
        }

        if (!s.hasPermission("mineskyitems.command."+args[0].toLowerCase())) {
            s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
            return true;
        }

        if (args[0].equalsIgnoreCase("criar-tinkerer")) {
            return TinkeringQueueCreator.processNextItem(s);
        }

        if(args[0].equalsIgnoreCase("reload")) {
            s.sendMessage("§7Recarregando arquivos...");
            CategoryHandler.categories.forEach(category -> {
                category.reloadFile();
                category.reloadCategory();
            });

            TinkeringManager.registerAllFromFile();

            s.sendMessage("§aCategorias recarregadas! "+ItemHandler.getItemsNames().size()+" itens ativos.");
            return true;
        }

        if (args[0].equalsIgnoreCase("force-add")) {
            if(args.length == 1) {
                s.sendMessage("§cInforme a categoria.");
                return true;
            }

            Category category = CategoryHandler.getCategory(args[1]);
            if(category == null) {
                s.sendMessage("§cCategoria não encontrada.");
                return true;
            }

            if(!(s instanceof Player p)) {
                s.sendMessage("§cApenas jogadores..");
                return true;
            }

            ItemStack itemStack = p.getInventory().getItemInMainHand();
            if(itemStack.getType().isAir()) {
                s.sendMessage("§cSegure um item válido para adicioná-lo.");
                return true;
            }

            PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
            final String name = plain.serialize(itemStack.getItemMeta().itemName());

            final PersistentDataContainer container = itemStack.getItemMeta().getPersistentDataContainer();

            final int model = itemStack.getItemMeta().hasCustomModelData() ? itemStack.getItemMeta().getCustomModelData() : 1;

            ItemBuilder builder = new ItemBuilder(category);
            builder.setItemLevel(container.getOrDefault(ItemHandler.LEVEL_NAMESPACE, PersistentDataType.INTEGER, 1));
            builder.setCustomModel(model);
            builder.setDisplayName(name);
            builder.setMaterial(itemStack.getType());

            Item item = builder.build();
            s.sendMessage("Item adicionado: "+item.getMetadata().displayName()+", level: "+item.getRequiredLevel());
            p.getInventory().setItemInMainHand(item.buildStack());
        }

        // give <player> <nome>
        if(args.length >= 3) {
            if(args[0].equalsIgnoreCase("give")) {
                if(!s.hasPermission("mineskyitems.command.give")) {
                    s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                    return true;
                }

                Player player = Bukkit.getPlayer(args[1]);
                if(player == null) {
                    s.sendMessage(Utils.c("&cNinguém encontrado com esse nick. Você pode usar /item get (nome) para dar o item a você mesmo."));
                    return true;
                }

                String itemName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                giveItemByName(player, itemName);

                return true;
            }
        }

        if(!(s instanceof Player p)) {
            s.sendMessage("§cApenas jogadores in-game podem utilizar esse comando.");
            return true;
        }

        if(args[0].equalsIgnoreCase("achar")) {
            if(!s.hasPermission("mineskyitems.command.achar")) {
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                return true;
            }

            Item item;

            // Comando possui input
            if(args.length >= 2) {
                String itemSearch = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

                item = ItemHandler.getItem(itemSearch);
                if(item != null) {
                    itemInfo(p, item);
                    return true;
                }

                p.sendMessage("§7Nenhum item encontrado basedo em seu input.");
            }

            p.sendMessage("§aVerificando item em sua mão primária...");
            item = ItemHandler.getItemFromStack(p.getInventory().getItemInMainHand());
            if(item == null) {
                p.sendMessage("§cNenhum item encontrado nem em sua mão e nem no prompt do item. Verifique com um Admin ou Desenvolvedor do plugin.");
                return true;
            }

            itemInfo(p, item);
            return true;
        }

        // criar, achar, get, editar, deletar, menu, script
        if(args.length >= 2) {
            final String prompt = args[1];

            if(!s.hasPermission("mineskyitems.command."+args[0])) {
                s.sendMessage("§cVocê não tem permissão ou o comando não existe.");
                return true;
            }

            if(args[0].equalsIgnoreCase("smelting")) {
                if(prompt.equalsIgnoreCase("reload")) {
                    SmeltingManager.loadRecipes();
                    s.sendMessage("§a✔ Receitas recarregadas!");
                    return true;
                }

                if(args.length == 2) {
                    s.sendMessage("§cInclua o ID do crafting.");
                    return true;
                }

                final String id = args[2].toLowerCase();

                if(prompt.equalsIgnoreCase("create")) {
                    SmeltingCreatorGUI gui = new SmeltingCreatorGUI(id);
                    p.openInventory(gui.getInventory());
                    return true;
                }

                if (prompt.equalsIgnoreCase("delete")) {
                    SmeltingManager.deleteRecipe(id);
                    s.sendMessage("§a✔ Receita '" + id + "' removida!");
                    return true;
                }

                return true;
            }

            if(args[0].equalsIgnoreCase("crafting")) {
                if(prompt.equalsIgnoreCase("reload")) {
                    CraftingManager.loadRecipes();
                    s.sendMessage("§a✔ Receitas recarregadas!");
                    return true;
                }

                if(args.length == 2) {
                    s.sendMessage("§cInclua o ID do crafting.");
                    return true;
                }

                final String id = args[2].toLowerCase();

                if(prompt.equalsIgnoreCase("create")) {
                    CraftingCreatorGUI gui = new CraftingCreatorGUI(id);
                    p.openInventory(gui.getInventory());
                    return true;
                }

                if (prompt.equalsIgnoreCase("delete")) {
                    CraftingManager.deleteRecipe(id);
                    s.sendMessage("§a✔ Receita '" + id + "' removida!");
                    return true;
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("tinkering")) {
                if (prompt.equalsIgnoreCase("reload")) {
                    TinkeringManager.registerAllFromFile();
                    s.sendMessage("§a✔ Receitas do Tinkering recarregadas!");
                    return true;
                }

                if (args.length < 3) {
                    s.sendMessage("§cInclua o ID do crafting.");
                    return true;
                }

                String id = args[2].toLowerCase();

                if (prompt.equalsIgnoreCase("create")) {
                    TinkeringCreatorGUI.openCreatorGUI(p, id);
                    return true;
                }

                if (prompt.equalsIgnoreCase("delete")) {
                    if (TinkeringManager.deleteRecipe(id)) {
                        s.sendMessage("§a✔ Receita '" + id + "' removida com sucesso!");
                    } else {
                        s.sendMessage("§c✘ Receita '" + id + "' não encontrada!");
                    }
                    return true;
                }

                return true;
            }

            if(args[0].equalsIgnoreCase("category")) {
                final Category category = CategoryHandler.getCategoryByName(args[1]);

                if (category == null) {
                    s.sendMessage("§cEssa categoria não existe!");
                    return true;
                }

                s.sendMessage("§6Nome: §f"+category.getName());
                s.sendMessage("§6ID: §f"+category.getId());
                s.sendMessage("§6Tipo: §f"+category.getType());
                s.sendMessage("§fListando todos os §e"+category.getAllItems().size()+" §fitens...");
                category.getAllItems().stream().sorted(Comparator.comparingInt(Item::getRequiredLevel))
                        .forEach(item -> {
                    s.sendMessage("§6• §e"+item.getMetadata().displayName()+"§f- Modelo: §a"+item.getMetadata().modelData()+"§f, Level: §d"+item.getRequiredLevel());
                });

                return true;
            }

            if(args[0].equalsIgnoreCase("get-all")) {
                final Category category = CategoryHandler.getCategoryByName(args[1]);

                if (category == null) {
                    s.sendMessage("§cEssa categoria não existe!");
                    return true;
                }

                category.getAllItems().stream().sorted(Comparator.comparingInt(Item::getRequiredLevel))
                        .forEach(item -> {
                            if (p.getInventory().firstEmpty() != -1) {
                                p.getInventory().addItem(item.buildStack());
                            } else {
                                p.getWorld().spawn(p.getLocation(), org.bukkit.entity.Item.class, it -> {
                                    it.setPickupDelay(15);
                                    it.setItemStack(item.buildStack());
                                });
                            }
                        });

                return true;
            }

            if(args[0].equalsIgnoreCase("get")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                giveItemByName((Player) s, itemName);

                return true;
            }

            if(args[0].equalsIgnoreCase("menu")) {
                if(prompt.equalsIgnoreCase("tinkering")) {
                    TinkeringGUI.openGUI(p, null);
                    return true;
                }

                if(prompt.equalsIgnoreCase("shop")) {
                    if(args.length == 2) {
                        s.sendMessage("Insira uma das classes: "+ RotatingItemsGUI.inventoryMap.keySet());
                        return true;
                    }

                    RotatingItemsGUI.openShop(p, args[2]);
                    return true;
                }
                if(prompt.equalsIgnoreCase("armors")) {
                    if(args.length == 2) {
                        s.sendMessage("Insira uma das classes: "+ RotatingItemsGUI.inventoryMap.keySet());
                        return true;
                    }

                    RotatingArmorsGUI.openShop(p, args[2]);
                    return true;
                }

                if(prompt.equalsIgnoreCase("destruir")) {
                    ItemRecyclerMenu.openMainMenu(p);
                } else {
                    ItemRepairMenu.openMainMenu(p);
                }

                return true;
            }

            if(args[0].equalsIgnoreCase("deletar")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                Item item = ItemHandler.getItemByName(itemName);
                if(item != null) {
                    ItemHandler.deleteItem(item);

                    s.sendMessage("§aItem deletado com sucesso!");
                    return true;
                }

                s.sendMessage("§cNenhum item existe com esse nome.");

                return true;
            }

            if(args[0].equalsIgnoreCase("script")) {
                final String scriptArgs = args.length > 2 ? args[2] : "";
                final String script = args[1].toLowerCase();

                switch(script) {
                    case "register-obtainings" -> {
                        File convertingFIle = new File(MineSkyItems.getInstance().getDataFolder(), "converting.yml");
                        if(!convertingFIle.exists()) {
                            s.sendMessage("nao exisdtre");
                            return true;
                        }

                        AtomicInteger converted = new AtomicInteger(0);

                        YamlConfiguration config = YamlConfiguration.loadConfiguration(convertingFIle);

                        List<String> naoAchou = new ArrayList<>();
                        Set<Category> categories = new HashSet<>();

                        for(String categoryId : config.getKeys(false)) {
                            ConfigurationSection categorySection = config.getConfigurationSection(categoryId);
                            if(categorySection == null)
                                continue;

                            for(String itemId : categorySection.getKeys(false)) {
                                final Item item = ItemHandler.getItemById(itemId);
                                if(item == null) {
                                    naoAchou.add(itemId);
                                    continue;
                                }

                                List<String> list = new ArrayList<>();
                                for(String obtId : categorySection.getStringList(itemId+".type")) {
                                    if(obtId.equals("MOB_DROPS"))
                                        obtId = "MOB_DROP";

                                    ObtainingMethod toGet = ObtainingMethod.fromValue(obtId);
                                    if(toGet == null)
                                        toGet = ObtainingMethod.UNKNOWN;

                                    list.add(toGet.name());
                                }

                                categories.add(item.getCategory());
                                item.getCategory().getConfig().set(item.getId()+".obtaining-methods", list);

                                converted.getAndIncrement();
                            }
                        }

                        Bukkit.getGlobalRegionScheduler().runDelayed(MineSkyItems.getInstance(), (task) -> {
                            for(Category category : categories) {
                                category.saveFile();
                            }

                            s.sendMessage(naoAchou.toString());
                            s.sendMessage("obtainings configurados: "+ converted.get());
                        }, 40);
                    }
                    case "empty" -> {
                        int startingFrom = 0;
                        if(args.length >= 4) {
                            startingFrom = Integer.parseInt(args[3]);
                        }

                        ItemFrameGenerator.generateEmpty(p.getLocation(), Material.getMaterial(scriptArgs), startingFrom);
                    }
                    case "convert" -> {
                        final Location origin = p.getLocation().getBlock().getLocation().add(0.5,0,0.5);

                        File converting = new File(MineSkyItems.getInstance().getDataFolder(), "converting.yml");
                        if(!converting.exists()) {
                            try {
                                converting.createNewFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        YamlConfiguration config = YamlConfiguration.loadConfiguration(converting);

                        AtomicInteger disableds = new AtomicInteger(0);
                        AtomicInteger converted = new AtomicInteger(0);
                        AtomicInteger alreadySet = new AtomicInteger(0);
                        for(int row = 0; row < 30; row++) {
                            for(int column = 0; column < 4; column++) {
                                final Location now = origin.clone().add(column,0, -(row));

                                final boolean isDisabled = now.getWorld().getType(now.clone().add(0,-1,0)) == Material.REDSTONE_BLOCK;
                                if(!isDisabled) {

                                    Bukkit.getRegionScheduler().run(MineSkyItems.getInstance(), now, (task) -> {
                                        Entity entity = now.getNearbyEntities(0.25, 0.5, 0.25).iterator().next();

                                        if (entity instanceof ItemFrame frame) {
                                            final ItemStack stack = frame.getItem();
                                            final Item item = ItemHandler.getItemFromStack(stack);

                                            if (item != null) {
                                                // good things here
                                                final String id = item.getId();
                                                final String categoryId = item.getCategory().getId();
                                                final String path = categoryId + "." + id;

                                                if(config.isSet(path)) {
                                                    alreadySet.getAndIncrement();
                                                    return;
                                                }

                                                final Material suggested = now.getWorld().getType(now.clone().add(0, 3, 0));
                                                if (!suggested.isAir()) {
                                                    config.set(path + ".suggested-material", suggested.name());
                                                }

                                                // types
                                                List<String> types = new ArrayList<>();
                                                for (int i = 0; i < 5; i++) {
                                                    final String type = now.getWorld().getType(now.clone().add(0, (5 + i), 0)).name();

                                                    if (type.contains("CHEST"))
                                                        types.add("loot");

                                                    if (type.contains("CRAFTING"))
                                                        types.add("crafting");

                                                    if (type.contains("SMITHING"))
                                                        types.add("upgrade");

                                                    if (type.contains("NETHERRACK"))
                                                        types.add("mob_drops");
                                                }

                                                p.sendMessage("Registering " + categoryId + ": " + id + ", types: " + types);
                                                converted.incrementAndGet();

                                                config.set(path + ".type", types);
                                            }
                                        }
                                    });
                                } else {
                                    disableds.getAndIncrement();
                                }
                            }
                        }

                        p.sendMessage("Salvando arquivos...");
                        Bukkit.getGlobalRegionScheduler().runDelayed(MineSkyItems.getInstance(), (task) -> {
                            try {
                                config.save(converting);
                                p.sendMessage("Salvo.");
                                p.sendMessage("Itens desativados: "+disableds.get());
                                p.sendMessage("Itens duplicados (pulados): "+alreadySet.get());
                                p.sendMessage("Itens convertidos: "+converted.get());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, 40);
                    }
                    case "single" -> {
                        int model = 0;
                        if(args.length >= 4) {
                            model = Integer.parseInt(args[3]);
                        }

                        ItemFrameGenerator.createItem(p.getLocation(), Material.getMaterial(scriptArgs), model);
                    }
                    case "category" -> ItemFrameGenerator.generateCategory(p.getLocation(), CategoryHandler.getCategory(scriptArgs));
                    case "armor" -> ArmorStandScript.generate(p.getLocation(), scriptArgs);

                    default -> p.sendMessage("Script não encontrado, scripts existentes: "+scripts);
                }

                return true;
            }

            if(args[0].equalsIgnoreCase("craft")) {
                return true;
            }

            if (args[0].equalsIgnoreCase("contar")) {
                p.sendMessage("Contando todos os itens...");

                int n = 0;
                for(Category category : CategoryHandler.categories) {
                    int size = category.getAllItems().size();
                    p.sendMessage(category.getName()+": "+size);

                    n = n + size;
                }

                p.sendMessage("Total de itens custom registrados: "+n);

                return true;
            }

            if(args[0].equalsIgnoreCase("danificar")) {
                int damage;

                try {
                    damage = Integer.parseInt(prompt);
                } catch (Exception ex) {
                    p.sendMessage("§cInforme um número válido de quantidade de dano para este item.");
                    return true;
                }

                Item item = ItemHandler.getItemFromStack(p.getInventory().getItemInMainHand());
                if(item == null) {
                    p.sendMessage("§cVocê deve segurar um item válido em sua mão para alterar a durabilidade dele.");
                    return true;
                }

                item.forceDamageItem(p, p.getInventory().getItemInMainHand(), damage);
                return true;
            }

            if(args[0].equalsIgnoreCase("criar")) {

                Category category = CategoryHandler.getCategory(prompt);

                if(category == null) {
                    s.sendMessage("Nenhuma categoria encontrada com esse Nome ou ID.");
                    return true;
                }

                s.sendMessage("§aCriando um novo item na categoria "+category.getName());
                ItemBuilderMenu.openMainMenu(p, new ItemBuilder(category));

                return true;
            }

            if(args[0].equalsIgnoreCase("editar")) {
                String itemName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                Item item = ItemHandler.getItemByName(itemName);
                if(item != null) {

                    ItemBuilderMenu.openMainMenu(p, new ItemBuilder(item));
                    return true;

                }

                s.sendMessage("§cNenhum item existe com esse nome.");
                return true;
            }

        }

        return false;
    }

    private static void itemInfo(Player p, Item item) {
        p.sendMessage("§6§lItem encontrado!");
        p.sendMessage("§6Nome: §e"+item.getMetadata().displayName());
        p.sendMessage("§6ID: §e"+item.getId());
        p.sendMessage("§6Level: §e"+item.getRequiredLevel());
        p.sendMessage("§6Categoria: §e"+item.getCategory().getName());

        Component component = Component.text("Clique aqui para editar esse item.")
                .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/item editar "+ChatColor.stripColor(item.getMetadata().displayName())));

        p.sendMessage(component);
    }

    private void giveItemByName(Player player, String name) {
        Item item = ItemHandler.getItemByName(name);
        if(item == null) {
            player.sendMessage(Utils.c("&cNenhum item encontrado com esse nome."));
            return;
        }

        player.getInventory().addItem(item.buildStack());
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command cmd, @NotNull String label, String[] args) {
        if(s instanceof Player p) {
            p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
        }

        if(args.length <= 1) {
            return subCommands;
        }

        if(args[0].equalsIgnoreCase("danificar")
        || args[0].equalsIgnoreCase("reload")) {
            return null;
        }

        if(args[0].equalsIgnoreCase("script")) {
            if(args.length == 2) {
                return scripts;
            } else if(args[1].equalsIgnoreCase("category")) {
                return CategoryHandler.getCategoriesNames();
            }
        }

        if(args[0].equalsIgnoreCase("menu")) {
            return menu_subCommands;
        }

        if(args[0].equalsIgnoreCase("achar")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNamesAndIds());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        if(args[0].equalsIgnoreCase("criar")
                || args[0].equalsIgnoreCase("get-all")
                || args[0].equalsIgnoreCase("force-add")
                || args[0].equalsIgnoreCase("category")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(CategoryHandler.getCategoriesNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        if(args[0].equalsIgnoreCase("get") || args[0].equalsIgnoreCase("deletar")) {
            String[] args2 = Arrays.copyOfRange(args, 1, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        final int length = args.length;
        final String type = args[0].toLowerCase();

        if (type.equals("crafting") || type.equals("tinkering") || type.equals("smelting")) {
            if (length == 2) {
                return craftingtinkering_subCommands;
            } else if (length == 3 && args[1].equalsIgnoreCase("delete")) {
                switch (type) {
                    case "crafting" -> {
                        return new ArrayList<>(CraftingManager.getRecipes().keySet());
                    }
                    case "tinkering" -> {
                        return new ArrayList<>(TinkeringManager.getRecipes().stream().map(TinkeringRecipe::getId).toList());
                    }
                    case "smelting" -> {
                        return new ArrayList<>(SmeltingManager.getRecipes().keySet());
                    }
                }
            }
            return List.of("<id>");
        }

        if(args[0].equalsIgnoreCase("give")) {
            if(args.length == 2) {
                return null;
            }

            String[] args2 = Arrays.copyOfRange(args, 3, args.length);

            List<String> e = new ArrayList<>(ItemHandler.getItemsNames());
            String input = String.join(" ", args2);

            reorganizeTabComplete(e, input, args2.length);

            return e;
        }

        //return List.of();
        return ItemHandler.getItemsNames();
    }

    private static void reorganizeTabComplete(List<String> list, String input, int length) {
        Iterator<String> iterator = list.iterator();
        List<String> elementosAdicionais = new ArrayList<>();

        while (iterator.hasNext()) {
            String elemento = iterator.next();
            String[] elementoArgs = elemento.split(" ");

            if (!elemento.toLowerCase().contains(input.toLowerCase())) {
                iterator.remove();
            } else {
                if (length >= 2) {
                    iterator.remove();
                    elementosAdicionais.add(elemento.toLowerCase().trim());
                }
            }
        }

        list.addAll(elementosAdicionais);
    }

}
