package hellfirepvp.beebetteratbees.client.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIClientUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.DefaultOverlayRenderer;
import codechicken.nei.api.IOverlayHandler;
import codechicken.nei.api.IRecipeOverlayRenderer;
import codechicken.nei.api.IStackPositioner;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;
import codechicken.nei.guihook.IContainerTooltipHandler;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.ICraftingHandler;
import codechicken.nei.recipe.IUsageHandler;
import codechicken.nei.recipe.RecipeInfo;

/**
 * HellFirePvP@Admin
 * Date: 28.04.2016 / 22:30
 * on BeeBetterAtBees
 * AbstractTreeGUIHandler
 */
public abstract class AbstractTreeGUIHandler implements ICraftingHandler, IUsageHandler {

    public int cycleticks = Math.abs((int) System.currentTimeMillis());
    public LinkedList<CachedRecipe> arecipes = new LinkedList<>();
    public LinkedList<RecipeTransferRect> transferRects = new LinkedList<>();

    public AbstractTreeGUIHandler() {
        loadTransferRects();
        RecipeTransferRectHandler.registerRectsToGuis(getRecipeTransferRectGuis(), this.transferRects);
    }

    private static boolean transferRect(GuiContainer gui, Collection<RecipeTransferRect> transferRects, int offsetx,
        int offsety, boolean usage) {
        Point pos = GuiDraw.getMousePosition();
        Point relMouse = new Point(pos.x - gui.width - offsetx, pos.y - gui.height - offsety);
        for (RecipeTransferRect rect : transferRects) {
            if ((rect.rect.contains(relMouse)) && (usage ? GuiUsageRecipe.openRecipeGui(rect.outputId, rect.results)
                : GuiCraftingRecipe.openRecipeGui(rect.outputId, rect.results))) {
                return true;
            }
        }
        return false;
    }

    private static List<String> transferRectTooltip(GuiContainer gui, Collection<RecipeTransferRect> transferRects,
        int offsetx, int offsety, List<String> currenttip) {
        Point pos = GuiDraw.getMousePosition();
        Point relMouse = new Point(pos.x - gui.width - offsetx, pos.y - gui.height - offsety);
        for (RecipeTransferRect rect : transferRects) {
            if (rect.rect.contains(relMouse)) {
                currenttip.add(NEIClientUtils.translate("recipe.tooltip"));
                break;
            }
        }
        return currenttip;
    }

    public abstract void loadCraftingRecipes(String outId, Object... results);

    public abstract void loadUsageRecipes(String inId, Object... ingredients);

    public String getOverlayIdentifier() {
        return null;
    }

    public List<Class<? extends GuiContainer>> getRecipeTransferRectGuis() {
        Class<? extends GuiContainer> clazz = getGuiClass();
        if (clazz != null) {
            LinkedList<Class<? extends GuiContainer>> list = new LinkedList<>();
            list.add(clazz);
            return list;
        }
        return null;
    }

    public Class<? extends GuiContainer> getGuiClass() {
        return null;
    }

    public AbstractTreeGUIHandler newInstance() {
        try {
            return getClass().getConstructor()
                .newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ICraftingHandler getRecipeHandler(String outputId, Object... results) {
        AbstractTreeGUIHandler handler = newInstance();
        handler.loadCraftingRecipes(outputId, results);
        return handler;
    }

    @Override
    public IUsageHandler getUsageHandler(String inputId, Object... ingredients) {
        AbstractTreeGUIHandler handler = newInstance();
        handler.loadUsageRecipes(inputId, ingredients);
        return handler;
    }

    @Override
    public int numRecipes() {
        return this.arecipes.size();
    }

    @Override
    public void drawBackground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GuiDraw.changeTexture(getGuiTexture());
        GuiDraw.drawTexturedModalRect(0, 0, 5, 11, 166, 65);
    }

    @Override
    public void drawForeground(int recipe) {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_LIGHTING);
        GuiDraw.changeTexture(getGuiTexture());
        drawExtras(recipe);
    }

    @Override
    public List<PositionedStack> getIngredientStacks(int recipe) {
        return this.arecipes.get(recipe)
            .getIngredients();
    }

    @Override
    public PositionedStack getResultStack(int recipe) {
        return this.arecipes.get(recipe)
            .getResult();
    }

    @Override
    public List<PositionedStack> getOtherStacks(int recipe) {
        return this.arecipes.get(recipe)
            .getOtherStacks();
    }

    @Override
    public void onUpdate() {
        if (!NEIClientUtils.shiftKey()) {
            this.cycleticks += 1;
        }
    }

    @Override
    public boolean hasOverlay(GuiContainer gui, Container container, int recipe) {
        return (RecipeInfo.hasDefaultOverlay(gui, getOverlayIdentifier()))
            || (RecipeInfo.hasOverlayHandler(gui, getOverlayIdentifier()));
    }

    @Override
    public IRecipeOverlayRenderer getOverlayRenderer(GuiContainer gui, int recipe) {
        IStackPositioner positioner = RecipeInfo.getStackPositioner(gui, getOverlayIdentifier());
        if (positioner == null) {
            return null;
        }
        return new DefaultOverlayRenderer(getIngredientStacks(recipe), positioner);
    }

    @Override
    public IOverlayHandler getOverlayHandler(GuiContainer gui, int recipe) {
        return RecipeInfo.getOverlayHandler(gui, getOverlayIdentifier());
    }

    @Override
    public int recipiesPerPage() {
        return 1;
    }

    @Override
    public List<String> handleTooltip(GuiRecipe<?> gui, List<String> currenttip, int recipe) {
        if (GuiContainerManager.shouldShowTooltip(gui) && currenttip.isEmpty()) {
            Point offset = gui.getRecipePosition(recipe);
            return transferRectTooltip(gui, this.transferRects, offset.x, offset.y, currenttip);
        }
        return currenttip;
    }

    @Override
    public List<String> handleItemTooltip(GuiRecipe<?> gui, ItemStack stack, List<String> currenttip, int recipe) {
        return currenttip;
    }

    @Override
    public boolean keyTyped(GuiRecipe<?> gui, char keyChar, int keyCode, int recipe) {
        if (keyCode == NEIClientConfig.getKeyBinding("gui.recipe")) {
            return transferRect(gui, recipe, false);
        }
        if (keyCode == NEIClientConfig.getKeyBinding("gui.usage")) {
            return transferRect(gui, recipe, true);
        }
        return false;
    }

    @Override
    public boolean mouseClicked(GuiRecipe<?> gui, int button, int recipe) {
        if (button == 0) {
            return transferRect(gui, recipe, false);
        }
        if (button == 1) {
            return transferRect(gui, recipe, true);
        }
        return false;
    }

    private boolean transferRect(GuiRecipe<?> gui, int recipe, boolean usage) {
        Point offset = gui.getRecipePosition(recipe);
        return transferRect(gui, this.transferRects, offset.x, offset.y, usage);
    }

    public void loadTransferRects() {}

    public abstract String getGuiTexture();

    public void drawExtras(int recipe) {}

    public static class RecipeTransferRect {

        Rectangle rect;
        String outputId;
        Object[] results;

        public RecipeTransferRect(Rectangle rectangle, String outputId, Object... results) {
            this.rect = rectangle;
            this.outputId = outputId;
            this.results = results;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RecipeTransferRect)) {
                return false;
            }
            return this.rect.equals(((RecipeTransferRect) obj).rect);
        }

        @Override
        public int hashCode() {
            return this.rect.hashCode();
        }
    }

    public static class RecipeTransferRectHandler implements IContainerInputHandler, IContainerTooltipHandler {

        private static final HashMap<Class<? extends GuiContainer>, HashSet<RecipeTransferRect>> guiMap = new HashMap<>();

        public static void registerRectsToGuis(List<Class<? extends GuiContainer>> classes,
            List<RecipeTransferRect> rects) {
            if (classes == null) {
                return;
            }
            for (Class<? extends GuiContainer> clazz : classes) {
                HashSet<RecipeTransferRect> set = guiMap.computeIfAbsent(clazz, k -> new HashSet<>());
                set.addAll(rects);
            }
        }

        public boolean canHandle(GuiContainer gui) {
            return guiMap.containsKey(gui.getClass());
        }

        @Override
        public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
            if (!canHandle(gui)) {
                return false;
            }
            if (keyCode == NEIClientConfig.getKeyBinding("gui.recipe")) {
                return transferRect(gui, false);
            }
            if (keyCode == NEIClientConfig.getKeyBinding("gui.usage")) {
                return transferRect(gui, true);
            }
            return false;
        }

        @Override
        public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
            if (!canHandle(gui)) {
                return false;
            }
            if (button == 0) {
                return transferRect(gui, false);
            }
            if (button == 1) {
                return transferRect(gui, true);
            }
            return false;
        }

        private boolean transferRect(GuiContainer gui, boolean usage) {
            int[] offset = RecipeInfo.getGuiOffset(gui);
            return AbstractTreeGUIHandler.transferRect(gui, guiMap.get(gui.getClass()), offset[0], offset[1], usage);
        }

        @Override
        public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

        @Override
        public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

        @Override
        public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

        @Override
        public boolean keyTyped(GuiContainer gui, char keyChar, int keyID) {
            return false;
        }

        @Override
        public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
            return false;
        }

        @Override
        public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

        @Override
        public List<String> handleTooltip(GuiContainer gui, int mousex, int mousey, List<String> currenttip) {
            if (!canHandle(gui)) {
                return currenttip;
            }
            if (GuiContainerManager.shouldShowTooltip(gui) && currenttip.isEmpty()) {
                int[] offset = RecipeInfo.getGuiOffset(gui);
                return transferRectTooltip(gui, guiMap.get(gui.getClass()), offset[0], offset[1], currenttip);
            }
            return currenttip;
        }

        @Override
        public List<String> handleItemDisplayName(GuiContainer gui, ItemStack itemstack, List<String> currenttip) {
            return currenttip;
        }

        @Override
        public List<String> handleItemTooltip(GuiContainer gui, ItemStack itemstack, int mousex, int mousey,
            List<String> currenttip) {
            return currenttip;
        }

        @Override
        public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}

    }

    static {
        GuiContainerManager.addInputHandler(new RecipeTransferRectHandler());
        GuiContainerManager.addTooltipHandler(new RecipeTransferRectHandler());
    }

}
