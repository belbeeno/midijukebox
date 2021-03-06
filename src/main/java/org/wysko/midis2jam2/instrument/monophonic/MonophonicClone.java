package org.wysko.midis2jam2.instrument.monophonic;

import com.jme3.scene.Node;
import org.wysko.midis2jam2.instrument.NotePeriod;

import java.util.ArrayList;
import java.util.List;

/**
 * Monophonic instruments ({@link MonophonicInstrument}) use MonophonicClones to visualize polyphony on monophonic
 * instruments. A clone is required for each degree of polyphony.
 *
 * @see MonophonicInstrument
 */
public abstract class MonophonicClone {
	
	/**
	 * The note periods for which this clone should be responsible for animating.
	 *
	 * @see NotePeriod
	 */
	public final List<NotePeriod> notePeriods;
	public final Node animNode = new Node();
	public final Node modelNode = new Node();
	public final Node cloneNode = new Node();
	/**
	 * The current note period that is being handled.
	 */
	public NotePeriod currentNotePeriod;
	/**
	 * Is this clone currently playing a note?
	 */
	protected boolean currentlyPlaying = false;
	
	public MonophonicClone() {
		this.notePeriods = new ArrayList<>();
	}
	
	public boolean isPlayingAtTime(long midiTick) {
		for (NotePeriod notePeriod : notePeriods) {
			if (midiTick >= notePeriod.startTick() && midiTick < notePeriod.endTick()) {
				return true;
			}
		}
		return false;
	}
	
	public abstract void tick(double time, float delta);
}
