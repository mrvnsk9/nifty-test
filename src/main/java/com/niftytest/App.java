package com.niftytest;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.builder.*;
import de.lessvoid.nifty.controls.ButtonClickedEvent;
import de.lessvoid.nifty.controls.button.builder.ButtonBuilder;
import de.lessvoid.nifty.nulldevice.NullSoundDevice;
import de.lessvoid.nifty.render.batch.BatchRenderDevice;
import de.lessvoid.nifty.renderer.lwjgl.input.LwjglInputSystem;
import de.lessvoid.nifty.renderer.lwjgl.render.LwjglBatchRenderBackendCoreProfileFactory;
import de.lessvoid.nifty.screen.DefaultScreenController;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import de.lessvoid.nifty.tools.Color;
import de.lessvoid.nifty.tools.SizeValue;
import org.lwjgl.opengl.*;
import org.lwjgl.util.glu.GLU;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Hello world!
 *
 */
public class App {
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 768;

    public static void main(final String[] args) throws Exception {
        initLWJGL();
        initGL();
        LwjglInputSystem inputSystem = initInput();
        Nifty nifty = initNifty(inputSystem);
        nifty.loadStyleFile("nifty-default-styles.xml");
        nifty.loadControlFile("nifty-default-controls.xml");
        createIntroScreen(nifty, new MyScreenController());
        nifty.gotoScreen("start");
        renderLoop(nifty);
        shutDown(inputSystem);
    }

    private static LwjglInputSystem initInput() throws Exception {
        LwjglInputSystem inputSystem = new LwjglInputSystem();
        inputSystem.startup();
        return inputSystem;
    }

    private static void initLWJGL() throws Exception {
        DisplayMode currentMode = Display.getDisplayMode();
        DisplayMode[] modes = Display.getAvailableDisplayModes();
        List<DisplayMode> matching = new ArrayList<>();
        for (DisplayMode mode : modes) {
            if (mode.getWidth() == WIDTH &&
                    mode.getHeight() == HEIGHT &&
                    mode.getBitsPerPixel() == 32) {
                matching.add(mode);
            }
        }

        DisplayMode[] matchingModes = matching.toArray(new DisplayMode[matching.size()]);
        boolean found = false;
        for (DisplayMode matchingMode : matchingModes) {
            if (matchingMode.getFrequency() == currentMode.getFrequency()) {
                Display.setDisplayMode(matchingMode);
                found = true;
                break;
            }
        }

        if (!found) {
            Arrays.sort(matchingModes, (o1, o2) -> {
                if (o1.getFrequency() > o2.getFrequency()) {
                    return 1;
                } else if (o1.getFrequency() < o2.getFrequency()) {
                    return -1;
                } else {
                    return 0;
                }
            });

            for (DisplayMode matchingMode : matchingModes) {
                Display.setDisplayMode(matchingMode);
                break;
            }
        }

        int x = (currentMode.getWidth() - Display.getDisplayMode().getWidth()) / 2;
        int y = (currentMode.getHeight() - Display.getDisplayMode().getHeight()) / 2;
        Display.setLocation(x, y);
        Display.setFullscreen(false);
        Display.create(new PixelFormat(), new ContextAttribs(3, 2).withProfileCore(true));
        Display.setVSyncEnabled(false);
        Display.setTitle("Hello Nifty");
    }

    private static void initGL() {
        glViewport(0, 0, Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight());
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glClear(GL11.GL_COLOR_BUFFER_BIT);
        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static Nifty initNifty(final LwjglInputSystem inputSystem) throws Exception {
        return new Nifty(
                new BatchRenderDevice(LwjglBatchRenderBackendCoreProfileFactory.create()),
                new NullSoundDevice(),
                inputSystem,
                new AccurateTimeProvider());
    }

    private static Screen createIntroScreen(final Nifty nifty, final ScreenController controller) {
        return new ScreenBuilder("start") {{
            controller(controller);
            layer(new LayerBuilder("layer") {{
                childLayoutCenter();
                onStartScreenEffect(new EffectBuilder("fade") {{
                    length(500);
                    effectParameter("start", "#0");
                    effectParameter("end", "#f");
                }});
                onEndScreenEffect(new EffectBuilder("fade") {{
                    length(500);
                    effectParameter("start", "#f");
                    effectParameter("end", "#0");
                }});
                onActiveEffect(new EffectBuilder("gradient") {{
                    effectValue("offset", "0%", "color", "#333f");
                    effectValue("offset", "100%", "color", "#ffff");
                }});
                panel(new PanelBuilder() {{
                    childLayoutVertical();
                    text(new TextBuilder() {{
                        text(getText());
                        wrap(true);
                        style("base-font");
                        color(Color.BLACK);
                        alignCenter();
                        valignCenter();
                    }});
                    panel(new PanelBuilder(){{
                        height(SizeValue.px(10));
                    }});
                    control(new ButtonBuilder("exit", "Exit") {{
                        alignCenter();
                        valignCenter();
                    }});
                }});
            }});
        }}.build(nifty);
    }

    private static String getText() {
        try {
            return new String(Files.readAllBytes(Paths.get(App.class.getClassLoader().getResource("text.txt").toURI())));
        } catch (IOException | URISyntaxException ignored) { }
        return "Nifty 1.4 Core Hello World";
    }

    private static void renderLoop(final Nifty nifty) {
        boolean done = false;
        while (!Display.isCloseRequested() && !done) {
            Display.update();
            if (nifty.update()) {
                done = true;
            }
            nifty.render(true);
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) {
                String glerrmsg = GLU.gluErrorString(error);
                System.err.println(glerrmsg);
            }
        }
    }

    private static void shutDown(final LwjglInputSystem inputSystem) {
        inputSystem.shutdown();
        Display.destroy();
        System.exit(0);
    }

    public static class MyScreenController extends DefaultScreenController {
        @NiftyEventSubscriber(id="exit")
        public void exit(final String id, final ButtonClickedEvent event) {
            nifty.exit();
        }
    }
}