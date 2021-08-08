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

package org.wysko.midis2jam2.instrument.family.brass;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.util.MatType;

import java.util.List;

import static org.wysko.midis2jam2.util.Utils.rad;

/**
 * The stage horns are positioned back and to the left. There are 12 of them for each note in the octave. Stage horns
 * are bouncy.
 *
 * @see BouncyTwelfth
 */
public class StageHorns extends WrappedOctaveSustained {
	
	/**
	 * The base position of a horn.
	 */
	private static final Vector3f BASE_POSITION = new Vector3f(0, 29.5F, -152.65F);
	
	/**
	 * Instantiates new stage horns.
	 *
	 * @param context   the context
	 * @param eventList the event list
	 */
	public StageHorns(Midis2jam2 context, List<MidiChannelSpecificEvent> eventList, StageHornsType type) {
		super(context, eventList, false);
		
		twelfths = new StageHornNote[12];
		var hornNodes = new Node[12];
		for (var i = 0; i < 12; i++) {
			hornNodes[i] = new Node();
			twelfths[i] = new StageHornNote(type);
			hornNodes[i].attachChild(twelfths[i].highestLevel);
			twelfths[i].highestLevel.setLocalTranslation(BASE_POSITION);
			hornNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(16 + i * 1.5), 0));
			instrumentNode.attachChild(hornNodes[i]);
		}
	}
	
	@Override
	protected void moveForMultiChannel(float delta) {
		for (TwelfthOfOctave twelfth : twelfths) {
			StageHornNote horn = (StageHornNote) twelfth;
			horn.highestLevel.setLocalTranslation(new Vector3f(BASE_POSITION).add(
					new Vector3f(0, 0, -5).mult(indexForMoving(delta))
			));
		}
	}
	
	/**
	 * A single horn.
	 */
	public class StageHornNote extends BouncyTwelfth {
		
		/**
		 * Instantiates a new stage horn note.
		 *
		 * @param type the type of stage horn
		 */
		public StageHornNote(StageHornsType type) {
			super();
			// Load horn
			animNode.attachChild(context.loadModel("StageHorn.obj", type.texture, MatType.REFLECTIVE, 0.9F));
		}
	}
	
	public enum StageHornsType {
		/**
		 * Brass section stage horns type.
		 */
		BRASS_SECTION("HornSkin.bmp"),
		/**
		 * Synth brass 1 stage horns type.
		 */
		SYNTH_BRASS_1("HornSkinGrey.bmp"),
		/**
		 * Synth brass 2 stage horns type.
		 */
		SYNTH_BRASS_2("HornSkinCopper.png");
		
		private final String texture;
		
		StageHornsType(String texture) {
			this.texture = texture;
		}
	}
}
