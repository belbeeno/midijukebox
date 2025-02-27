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
package org.wysko.midis2jam2.instrument.family.percussive

import com.jme3.math.Quaternion
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent
import org.wysko.midis2jam2.util.Utils.rad

/** The Melodic tom. */
class MelodicTom(
    context: Midis2jam2,
    eventList: List<MidiChannelSpecificEvent>
) : OneDrumOctave(context, eventList) {

    override val strikers: Array<Striker> = Array(12) { i ->
        Striker(
            context = context,
            strikeEvents = eventList.modulus(i),
            stickModel = StickType.DRUMSET_STICK
        ).apply {
            setParent(recoilNode)
            node.setLocalTranslation(1.8f * (i - 5.5f), 0f, 15f)
            offsetStick { it.move(0f, 0f, -5f) }
        }
    }

    override fun moveForMultiChannel(delta: Float) {
        offsetNode.localRotation = Quaternion().fromAngles(0f, rad(-26.3 + updateInstrumentIndex(delta) * -15), 0f)
    }

    init {
        recoilNode.attachChild(
            context.loadModel("MelodicTom.obj", "DrumShell_MelodicTom.bmp").apply {
                localRotation = Quaternion().fromAngles(rad(36.0), 0f, 0f)
            }
        )
        instrumentNode.setLocalTranslation(0f, 61.1f, -133.8f)
    }
}
