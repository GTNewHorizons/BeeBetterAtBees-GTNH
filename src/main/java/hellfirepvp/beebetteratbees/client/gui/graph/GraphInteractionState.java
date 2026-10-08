package hellfirepvp.beebetteratbees.client.gui.graph;

// zoom + pan for the bee tree, nothing fancy
public class GraphInteractionState {

    private float zoomLevel = 1.0f;
    private float panX = 0.0f;
    private float panY = 0.0f;

    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 3.0f;
    private static final float ZOOM_STEP = 0.1f;

    public GraphInteractionState() {
        resetViewport();
    }

    // wheel up = zoom in, wheel down = zoom out
    public void zoom(float delta) {
        float newZoom = zoomLevel + (delta * ZOOM_STEP);
        zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
    }

    // shift the view around
    public void pan(float deltaX, float deltaY) {
        panX += deltaX;
        panY += deltaY;
    }

    // world coords -> where it shows on screen
    public float[] worldToScreen(float worldX, float worldY) {
        float screenX = (worldX * zoomLevel) + panX;
        float screenY = (worldY * zoomLevel) + panY;
        return new float[] { screenX, screenY };
    }

    // other way around, for mouse pos
    public float[] screenToWorld(float screenX, float screenY) {
        float worldX = (screenX - panX) / zoomLevel;
        float worldY = (screenY - panY) / zoomLevel;
        return new float[] { worldX, worldY };
    }

    // back to default view
    public void resetViewport() {
        zoomLevel = 1.0f;
        panX = 0.0f;
        panY = 0.0f;
    }

    public float getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(float zoom) {
        zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }

    public float getPanX() {
        return panX;
    }

    public float getPanY() {
        return panY;
    }

    public void setPan(float x, float y) {
        panX = x;
        panY = y;
    }
}
