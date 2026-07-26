package net.mineskyitems.gui.tinkering.recipe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Map;

public class TinkeringRecipe {

    private final String id;
    private final RecipeManager.ItemEntry[][] croppedRecipeGrid;
    private final RecipeManager.ItemEntry result;

    public TinkeringRecipe(String id, String[] shape, Map<Character, RecipeManager.ItemEntry> ingredients, RecipeManager.ItemEntry result) {
        this.id = id;
        this.result = result;
        this.croppedRecipeGrid = computeCroppedRecipe(shape, ingredients);
    }

    private RecipeManager.ItemEntry[][] computeCroppedRecipe(String[] shape, Map<Character, RecipeManager.ItemEntry> ingredients) {
        RecipeManager.ItemEntry[][] matrix = new RecipeManager.ItemEntry[5][5];
        for (int r = 0; r < 5; r++) {
            String rowString = r < shape.length ? shape[r] : "     ";
            for (int c = 0; c < 5; c++) {
                char keyChar = c < rowString.length() ? rowString.charAt(c) : ' ';
                matrix[r][c] = (keyChar == ' ') ? null : ingredients.get(keyChar);
            }
        }
        return cropMatrix(matrix);
    }

    private RecipeManager.ItemEntry[][] cropMatrix(RecipeManager.ItemEntry[][] matrix) {
        int minRow = 5, maxRow = -1;
        int minCol = 5, maxCol = -1;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                RecipeManager.ItemEntry mat = matrix[r][c];
                if (mat != null && !mat.isAir()) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }

        if (maxRow == -1) {
            return new RecipeManager.ItemEntry[0][0]; // Receita vazia
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        RecipeManager.ItemEntry[][] cropped = new RecipeManager.ItemEntry[height][width];

        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                cropped[r][c] = matrix[minRow + r][minCol + c];
            }
        }

        return cropped;
    }

    public String getId() {
        return id;
    }

    public RecipeManager.ItemEntry[][] getCroppedRecipeGrid() {
        return croppedRecipeGrid;
    }

    public RecipeManager.ItemEntry getResult() {
        return result;
    }
}