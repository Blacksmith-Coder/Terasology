/*
 * Copyright 2016 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.dag.nodes;

import org.lwjgl.opengl.GL11;
import org.terasology.config.Config;
import org.terasology.config.RenderingDebugConfig;
import org.terasology.engine.ComponentSystemManager;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.monitoring.PerformanceMonitor;
import org.terasology.registry.In;
import org.terasology.rendering.cameras.Camera;
import org.terasology.rendering.dag.ConditionDependentNode;
import org.terasology.rendering.dag.WireframeCapable;
import org.terasology.rendering.dag.WireframeTrigger;
import org.terasology.rendering.dag.stateChanges.SetViewportToSizeOf;
import org.terasology.rendering.dag.stateChanges.SetWireframe;
import org.terasology.rendering.world.WorldRenderer;

import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.terasology.rendering.opengl.DefaultDynamicFBOs.READ_ONLY_GBUFFER;

/**
 * TODO: Diagram of this node
 */
public class FirstPersonViewNode extends ConditionDependentNode implements WireframeCapable {

    @In
    private WorldRenderer worldRenderer;

    @In
    private Config config;

    @In
    private ComponentSystemManager componentSystemManager;

    private Camera playerCamera;
    private RenderingDebugConfig renderingDebugConfig;
    private SetWireframe wireframeStateChange;

    @Override
    public void initialise() {
        playerCamera = worldRenderer.getActiveCamera();

        wireframeStateChange = new SetWireframe(true);
        renderingDebugConfig = config.getRendering().getDebug();
        new WireframeTrigger(renderingDebugConfig, this);

        addDesiredStateChange(new SetViewportToSizeOf(READ_ONLY_GBUFFER));
    }

    public void enableWireframe() {
        if (!getDesiredStateChanges().contains(wireframeStateChange)) {
            addDesiredStateChange(wireframeStateChange);
            refreshTaskList();
        }
    }

    public void disableWireframe() {
        if (getDesiredStateChanges().contains(wireframeStateChange)) {
            removeDesiredStateChange(wireframeStateChange);
            refreshTaskList();
        }
    }

    @Override
    public void process() {
        if (!renderingDebugConfig.isFirstPersonElementsHidden()) {
            PerformanceMonitor.startActivity("rendering/firstPersonView");

            READ_ONLY_GBUFFER.bind(); // TODO: to be removed - will eventually be bound with a state change

            /*
             * Sets the state to render the First Person View.
             *
             * This generally comprises the objects held in hand, i.e. a pick, an axe, a torch and so on.
             */
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glDepthFunc(GL11.GL_ALWAYS);

            playerCamera.updateMatrices(90f);
            playerCamera.loadProjectionMatrix();

            for (RenderSystem renderer : componentSystemManager.iterateRenderSubscribers()) {
                renderer.renderFirstPerson();
            }

            playerCamera.updateMatrices();
            playerCamera.loadProjectionMatrix();

            /*
             * Resets the state after the render of the First Person View.
             */
            GL11.glDepthFunc(GL_LEQUAL);
            GL11.glPopMatrix();

            PerformanceMonitor.endActivity();
        }
    }
}
