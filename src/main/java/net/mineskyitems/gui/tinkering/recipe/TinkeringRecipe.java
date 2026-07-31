package net.mineskyitems.gui.tinkering.recipe;

import java.util.Map;

public class TinkeringRecipe {

    private final String id;
    private final TinkeringManager.ItemEntry[][] croppedRecipeGrid;
    private final TinkeringManager.ItemEntry result;

    public TinkeringRecipe(String id, String[] shape, Map<Character, TinkeringManager.ItemEntry> ingredients, TinkeringManager.ItemEntry result) {
        this.id = id;
        this.result = result;
        this.croppedRecipeGrid = computeCroppedRecipe(shape, ingredients);
    }

    private TinkeringManager.ItemEntry[][] computeCroppedRecipe(String[] shape, Map<Character, TinkeringManager.ItemEntry> ingredients) {
        TinkeringManager.ItemEntry[][] matrix = new TinkeringManager.ItemEntry[5][5];
        for (int r = 0; r < 5; r++) {
            String rowString = r < shape.length ? shape[r] : "     ";
            for (int c = 0; c < 5; c++) {
                char keyChar = c < rowString.length() ? rowString.charAt(c) : ' ';
                matrix[r][c] = (keyChar == ' ') ? null : ingredients.get(keyChar);
            }
        }
        return cropMatrix(matrix);
    }

    private TinkeringManager.ItemEntry[][] cropMatrix(TinkeringManager.ItemEntry[][] matrix) {
        int minRow = 5, maxRow = -1;
        int minCol = 5, maxCol = -1;

        for (int r = 0; r < 5; r++) {
            for (int c = 0; c < 5; c++) {
                TinkeringManager.ItemEntry mat = matrix[r][c];
                if (mat != null && !mat.isAir()) {
                    minRow = Math.min(minRow, r);
                    maxRow = Math.max(maxRow, r);
                    minCol = Math.min(minCol, c);
                    maxCol = Math.max(maxCol, c);
                }
            }
        }

        if (maxRow == -1) {
            return new TinkeringManager.ItemEntry[0][0]; // vazio
        }

        int height = maxRow - minRow + 1;
        int width = maxCol - minCol + 1;
        TinkeringManager.ItemEntry[][] cropped = new TinkeringManager.ItemEntry[height][width];

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

    public TinkeringManager.ItemEntry[][] getCroppedRecipeGrid() {
        return croppedRecipeGrid;
    }

    public TinkeringManager.ItemEntry getResult() {
        return result;
    }
}