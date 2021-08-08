/*
 * Copyright (C) 2021 Jacob Wysko
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

package org.wysko.midis2jam2.instrument.family.percussion.drumset;

import com.jme3.scene.Spatial;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator;
import org.wysko.midis2jam2.instrument.family.percussive.Stick;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;
import org.wysko.midis2jam2.util.MatType;
import org.wysko.midis2jam2.world.Axis;

import java.util.List;

import static org.wysko.midis2jam2.instrument.family.percussive.Stick.MAX_ANGLE;
import static org.wysko.midis2jam2.instrument.family.percussive.Stick.STRIKE_SPEED;

/**
 * The ride cymbal.
 */
public class RideCymbal extends Cymbal {
	
	/**
	 * Instantiates a new Ride cymbal.
	 *
	 * @param context the context
	 * @param hits    the hits
	 * @param type    the type
	 */
	public RideCymbal(Midis2jam2 context,
	                  List<MidiNoteOnEvent> hits, Cymbal.CymbalType type) {
		super(context, hits, type);
		if (!(type == CymbalType.RIDE_1 || type == CymbalType.RIDE_2)) {
			throw new IllegalArgumentException("Ride cymbal type is wrong.");
		}
		
		final Spatial cymbal = context.loadModel("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp", MatType.REFLECTIVE, 0.7f);
		cymbalNode.attachChild(cymbal);
		cymbalNode.setLocalScale(type.getSize());
		highLevelNode.setLocalTranslation(type.getLocation());
		highLevelNode.setLocalRotation(type.getRotation());
		highLevelNode.attachChild(cymbalNode);
		stickNode.setLocalTranslation(0, 0, 20);
		this.animator = new CymbalAnimator(type.getAmplitude(), type.getWobbleSpeed(), type.getDampening());
	}
	
	@Override
	public void tick(double time, float delta) {
		var stickStatus = handleStick(time, delta, hits);
		handleCymbalStrikes(time, delta, stickStatus.justStruck());
	}
	
	@Override
	Stick.StickStatus handleStick(double time, float delta, List<MidiNoteOnEvent> hits) {
		var stickStatus = Stick.handleStick(context, stick, time, delta, hits, STRIKE_SPEED, MAX_ANGLE, Axis.X);
		var strikingFor = stickStatus.strikingFor();
		if (strikingFor != null) {
			stickNode.setLocalTranslation(0, 0, strikingFor.note == 53 ? 15 : 20);
		}
		return stickStatus;
	}
}
