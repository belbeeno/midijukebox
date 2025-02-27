/*
 * Copyright (C) 2022 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */
package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.NonDrumSetPercussion
import org.wysko.midis2jam2.instrument.family.percussion.drumset.PercussionInstrument
import org.wysko.midis2jam2.util.cullHint

/** How fast instruments move when transitioning. */
private const val BASE_TRANSITION_SPEED = 2500

/**
 * Any visual representation of a MIDI instrument. midis2jam2 displays separate instruments for
 * each channel, and also creates new instruments when the program of a channel changes (i.e., the MIDI instrument of
 * the channel changes).
 *
 * Classes that implement Instrument are responsible for handling [tick], which updates the current animation and
 * note handling for every call.
 */
abstract class Instrument protected constructor(
    /** Context to the main class. */
    val context: Midis2jam2
) {

    /** Used for moving the instrument when there are two or more consecutively visible instruments of the same type. */
    val offsetNode: Node = Node()

    /** Used for general positioning and rotation of the instrument. */
    val highestLevel: Node = Node()

    /** Contains instrument geometry, ideally through further sub-nodes. */
    val instrumentNode: Node = Node()

    /**
     * When true, this instrument should be displayed on the screen. Otherwise, it should not. The positions of
     * instruments rely on this variable.
     */
    var isVisible: Boolean = false
        set(value) {
            if (context.properties.getProperty("never_hidden").equals("true", ignoreCase = true)) {
                context.rootNode.attachChild(offsetNode)
                field = value
                return
            }

            if (value) {
                context.rootNode.attachChild(offsetNode)
            } else {
                context.rootNode.detachChild(offsetNode)
            }

            if (this is PercussionInstrument && this !is NonDrumSetPercussion) {
                field = true
                instrumentNode.cullHint = Spatial.CullHint.Dynamic
                return
            }

            instrumentNode.cullHint = value.cullHint()
            field = value
        }
        get() {
            return if (context.properties.getProperty("never_hidden").equals("true", ignoreCase = true)) {
                true
            } else field
        }

    /**
     * The index of this instrument in the stack of similar instruments. Can be a decimal when instrument transition
     * easing is enabled.
     */
    private var index = 0.0

    init {
        /* Connect node tree */
        highestLevel.attachChild(instrumentNode)
        offsetNode.attachChild(highestLevel)
        context.rootNode.attachChild(offsetNode)
    }

    /**
     * Updates note collection, animation, visibility, and any other calculations that need to run on each frame.
     *
     * @param time  the current time since the beginning of the MIDI file, expressed in seconds
     * @param delta the amount of time since the last call this method, expressed in seconds
     */
    abstract fun tick(time: Double, delta: Float)

    /**
     * Calculates if this instrument is visible at a given time. Implementations of this method should follow this
     * general guideline:
     *
     * * If the instrument is currently playing, it should be visible.
     * * Otherwise, if there is less than or equal to one second from the current time until the next note, it should
     *   be visible.
     * * Otherwise, if there is less than or equal to seven seconds from the last played note and next note to play,
     *   it should be visible.
     * * Otherwise, if there is less than or equal to two seconds since the last previously played note, it should be
     *   visible.
     * * Otherwise, it should be invisible.
     */
    abstract fun calcVisibility(time: Double, future: Boolean = false): Boolean

    /**
     * Returns the index of this instrument in the list of other instruments of this type that are visible.
     *
     * @param delta the amount of time that has passed since the last frame
     */
    @Contract(pure = false)
    protected fun updateInstrumentIndex(delta: Float): Float {
        val similarAndVisible = similarVisible()
        val targetIndex = if (isVisible) {
            /* Index in the list of instruments from context */
            similarAndVisible.indexOf(this).coerceAtLeast(0)
        } else {
            /* The number of visible instruments of this type, minus one */
            similarAndVisible.size - 1
        }

        /* Update the index gradually to the target index, given the transition speed */
        index += delta * BASE_TRANSITION_SPEED * (targetIndex - index) / 500.0

        /* Never set the instrument index to anything larger than the number of instruments of this type */
        index = index.coerceAtMost(similar().size.toDouble())

        return index.toFloat()
    }

    /** The number of instruments that are [similar] and are visible. */
    protected open fun similarVisible(): List<Instrument> = similar().filter { it.isVisible }

    /**
     * Returns a list of instruments that should stack with this instrument (typically those of the same class or
     * subclasses).
     */
    protected open fun similar(): List<Instrument> = context.instruments.filter { this.javaClass.isInstance(it) }

    /** Does the same thing as [updateInstrumentIndex] but is pure and does not modify any variables. */
    @Contract(pure = true)
    fun checkInstrumentIndex(): Double = index

    /** Calculates and moves this instrument for when multiple instances of this instrument are visible. */
    protected abstract fun moveForMultiChannel(delta: Float)

    /**
     * Given the current [time], calls [calcVisibility] to determine the current visibility, updating [isVisible] and
     * the cull hint of [instrumentNode].
     */
    protected fun setVisibility(time: Double) {
        isVisible = calcVisibility(time)
    }

    /** Formats a property about this instrument for debugging purposes. */
    protected fun debugProperty(name: String, value: String): String {
        return "\t- $name: $value\n"
    }

    /** Formats a property about this instrument for debugging purposes. */
    protected fun debugProperty(name: String, value: Float): String {
        return "\t- $name: ${"%.3f".format(value)}\n"
    }

    override fun toString(): String {
        return buildString {
            append("* ${this@Instrument.javaClass.simpleName} / ${"%.3f".format(checkInstrumentIndex())}\n")
        }
    }
}
