package com.soliddowant.gregtechenergistics.integration.jei;

import appeng.container.implementations.ContainerPatternTerm;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferRegistry;
import mezz.jei.config.Constants;
import mezz.jei.recipes.RecipeTransferRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@SuppressWarnings({"unused", "rawtypes"})
@JEIPlugin
public class JeiPlugin implements IModPlugin {
    @Override
    public void register(IModRegistry registry)
    {
        IRecipeTransferRegistry transferRegistry = registry.getRecipeTransferRegistry();

        // If true, some change to JEI broke this integration
        if(!(transferRegistry instanceof RecipeTransferRegistry))
            return;

        RecipeTransferRegistry castedTransferRegistry = (RecipeTransferRegistry) transferRegistry;

        // Collect non-ContainerPatternTerm transfer handlers and flag if AE2 has been found
        // JEE checks if AE2 has been found, but that's not needed as the JVM would throw an exception on the import
        // of ContainerPatternTerm if AE2 was not present
        Table<Class<?>, String, IRecipeTransferHandler> collectedRegistry =
                collectNonPatternTerminals(castedTransferRegistry.getRecipeTransferHandlers());

        collectedRegistry.put(ContainerPatternTerm.class, Constants.UNIVERSAL_RECIPE_TRANSFER_UID,
                new RecipeTransferHandler());
        ReflectionHelper.setPrivateValue(RecipeTransferRegistry.class,
                (RecipeTransferRegistry) registry.getRecipeTransferRegistry(), collectedRegistry,
                "recipeTransferHandlers", null);
    }

    protected static <T, U> Table<Class<?>, T, U> collectNonPatternTerminals(ImmutableTable<Class, T, U> transferHandlers) {
        Table<Class<?>, T, U> newRegistry = HashBasedTable.create();
        for (final Table.Cell<Class, T, U> currentCell : transferHandlers.cellSet()) {
            Class<?> rowKey = currentCell.getRowKey();
            if(rowKey == null)
                continue;

            if (currentCell.getRowKey().equals(ContainerPatternTerm.class)) {
                continue;
            }

            //noinspection ConstantConditions
            newRegistry.put(currentCell.getRowKey(), currentCell.getColumnKey(), currentCell.getValue());
        }

        return newRegistry;
    }
}
