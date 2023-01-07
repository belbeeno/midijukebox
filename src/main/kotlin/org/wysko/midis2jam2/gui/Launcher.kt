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
@file:Suppress("kotlin:S1128")

package org.wysko.midis2jam2.gui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ExperimentalGraphicsApi
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.CONFIGURATION_DIRECTORY
import org.wysko.midis2jam2.midi.search.MIDISearchFrame
import org.wysko.midis2jam2.starter.Execution
import org.wysko.midis2jam2.util.ThrowableDisplay.display
import org.wysko.midis2jam2.util.Utils
import org.wysko.midis2jam2.util.logger
import java.awt.Cursor
import java.awt.Dimension
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.MessageFormat
import java.util.Locale
import java.util.Properties
import java.util.ResourceBundle
import javax.sound.midi.MidiSystem
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter

/**
 * A list of locales that the launcher supports.
 */
private val supportedLocales: List<Locale> = Utils.resourceToString("/supported-i18n.txt").split("\n").map {
    Locale.forLanguageTag(it)
}

/**
 * The user's home directory.
 */
val USER_HOME_DIRECTORY: File = File(System.getProperty("user.home"))

/**
 * Defines the default state of the launcher's GUI components.
 */
private val DEFAULT_LAUNCHER_STATE = Properties().apply {
    setProperty("locale", "en")
    setProperty("midi_device", "Gervill")
    setProperty("lastdir", USER_HOME_DIRECTORY.absolutePath)
    setProperty("soundfonts", Json.encodeToString(listOf<String>()))
}

/**
 * Returns the file that stores the settings. If the file or directories to it don't exist, they are created whenever
 * this method is called.
 */
private val LAUNCHER_STATE_FILE: File
    get() = File(CONFIGURATION_DIRECTORY, "launcher.properties").also {
        if (!it.exists()) {
            it.parentFile.mkdirs()
            it.createNewFile()
        }
    }

/**
 * The current state of the launcher properties.
 *
 * This anonymous object will write any changes to the properties to the [LAUNCHER_STATE_FILE] whenever the
 * [Properties.setProperty] method is called.
 */
val launcherState: LauncherProperties = LauncherProperties(DEFAULT_LAUNCHER_STATE).apply {
    load(FileReader(LAUNCHER_STATE_FILE))
}

private val soundbankFileFilter = object : FileFilter() {
    override fun accept(file: File?): Boolean =
        file?.extension?.lowercase(Locale.getDefault()) == "sf2" || file?.extension?.lowercase(Locale.getDefault()) == "dls" || file?.isDirectory == true

    override fun getDescription(): String = "Soundbank files (*.sf2, *.dls)"
}

var launcherSelectedMIDIFile: File? = null

/**
 * Displays configuration options and settings for midis2jam2.
 */
@ExperimentalGraphicsApi
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@Composable
@Suppress("FunctionName", "kotlin:S3776")
fun Launcher(): LauncherController {
    /* Internationalization */
    var locale by remember { mutableStateOf(Internationalization.locale) }
    val i18n by remember {
        derivedStateOf {
            @Suppress("UNUSED_EXPRESSION")
            locale
            Internationalization.i18n
        }
    }

    /* MIDI File */
    var selectedMIDIFile: File? by remember { mutableStateOf(null) }
    var playlist: File? by remember { mutableStateOf(null) }

    /* Playlist pointer */
    var playlistPointer by remember { mutableStateOf(0) }

    /* MIDI Device */
    val midiDevices = MidiSystem.getMidiDeviceInfo().map { it.name }.toList().filter { it != "Real Time Sequencer" }
    var selectedMidiDevice by remember { mutableStateOf(launcherState.getProperty("midi_device")) }

    /* SoundFont */
    val soundFonts by remember {
        derivedStateOf {
            val decodeFromString =
                Json.decodeFromString<List<String>>(launcherState.getProperty("soundfonts")).map { File(it).name }
            decodeFromString.ifEmpty { listOf("Default SoundFont") }.toMutableList()
        }
    }
    var selectedSoundFont by remember { mutableStateOf(0) }

    /* Navigation */
    var showAbout by remember { mutableStateOf(false) }
    var showOSS by remember { mutableStateOf(false) }
    val ossRotation by animateFloatAsState(if (showOSS) 180f else 0f)
    val screenScroll = ScrollState(0)

    /* Display */
    val width = 500.dp
    var freeze by remember { mutableStateOf(false) }
    var thinking by remember { mutableStateOf(false) }

    var beginMidis2jam2PlaylistFwd : (() -> UInt)? = null

    fun beginMidis2jam2() {
        try {
            Execution.start(
                properties = Properties().apply {
                    setProperty("midi_file", selectedMIDIFile?.absolutePath)
                    setProperty("midi_device", selectedMidiDevice)

                    if (soundFonts[selectedSoundFont] != "Default SoundFont") {
                        setProperty(
                            "soundfont",
                            launcherState.getSoundFontFile(soundFonts[selectedSoundFont]).absolutePath
                        )
                    }
                },
                onStart = {
                    freeze = true
                    thinking = true
                    MIDISearchFrame.lock()
                },
                onReady = {
                    thinking = false
                },
                onFinish = {
                    freeze = false
                    thinking = false
                    if (playlist != null) {
                        beginMidis2jam2PlaylistFwd!!.invoke()
                        println("Invoke called!")
                    }
                    else {
                        println("Unlocking MidiSearchFrame")
                        MIDISearchFrame.unlock()                        
                    }
                }
            )
        } catch (e: Throwable) {
            freeze = false
            thinking = false
            e.display()
        }
    }

    beginMidis2jam2PlaylistFwd = {
        println("FGSFDS - This is a directory!")
        val files = playlist!!.listFiles()
        var nextTime = Integer.MAX_VALUE
        var nextPath = ""
        for (file in files) {
            val timeIterator = Integer.parseInt(file.nameWithoutExtension.split("-")[0])
            if (timeIterator > playlistPointer && timeIterator < nextTime) {
                nextTime = timeIterator
                nextPath = file.getAbsolutePath()
                println(timeIterator)
            }
        }
        println(nextPath)
        println("---")
        if (nextTime != Integer.MAX_VALUE) {
            playlistPointer = nextTime
            selectedMIDIFile = File(nextPath)
            beginMidis2jam2()
            println("FGSFEDS done")
        }
        else {
            selectedMIDIFile = playlist
            playlist = null
            println("Playlist is over")
        }
        0u
    }

    var midiFileTextField: ((File) -> Unit)? = null

    @Composable
    fun NumberTextField() {
        var text: String? by remember { mutableStateOf("") }
        TextField(
            value = text!!,
            onValueChange = {
                try {
                    println("FGSFDS trying to parse the value " + it.toString())
                    playlistPointer = Integer.parseInt(it.toString())
                }
                catch (ex : NumberFormatException) {
                    println("FAILURE " + ex.toString())
                    playlistPointer = 0
                }
                text = playlistPointer.toString()
            },
            singleLine = true,
            modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp).width(500.dp),
            label = { Text("Playlist Position") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }

    MaterialTheme(darkColors()) {
        Surface(
            Modifier.fillMaxSize().pointerHoverIcon(
                if (thinking) {
                    PointerIcon(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
                } else {
                    PointerIconDefaults.Default
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(width)
                ) {
                    Image(
                        bitmap = useResource("logo.png", ::loadImageBitmap),
                        contentDescription = i18n.getString("midis2jam2"),
                        modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp).clickable {
                            showAbout = !showAbout
                        }
                    )
                    AnimatedVisibility(
                        visible = !showAbout
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(width).verticalScroll(screenScroll)
                        ) {
                            Text(
                                text = i18n.getString("configuration.configuration"),
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp)
                            )
                            midiFileTextField = MIDIFileTextField(i18n) {
                                selectedMIDIFile = it
                                launcherSelectedMIDIFile = it
                            }
                            NumberTextField()
                            SimpleExposedDropDownMenu(
                                values = midiDevices,
                                selectedIndex = midiDevices.indexOf(selectedMidiDevice),
                                onChange = {
                                    midiDevices[it].let { device ->
                                        launcherState.setProperty("midi_device", device)
                                        selectedMidiDevice = device
                                    }
                                },
                                label = { Text(i18n.getString("configuration.midi_device")) },
                                modifier = Modifier.width(width).padding(0.dp, 0.dp, 0.dp, 16.dp)
                            )
                            AnimatedVisibility(
                                visible = selectedMidiDevice == "Gervill"
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(width).fillMaxHeight().padding(0.dp, 0.dp, 0.dp, 16.dp)
                                ) {
                                    SimpleExposedDropDownMenu(
                                        values = soundFonts,
                                        selectedIndex = selectedSoundFont,
                                        onChange = { selectedSoundFont = it },
                                        label = { Text(i18n.getString("configuration.soundfont")) },
                                        modifier = Modifier.width(428.dp).padding(0.dp, 0.dp, 0.dp, 0.dp)
                                    )
                                    Button(
                                        onClick = {
                                            JFileChooser().run {
                                                fileFilter = soundbankFileFilter

                                                preferredSize = Dimension(800, 600)
                                                dialogTitle = "Select soundbank file"
                                                isMultiSelectionEnabled = false
                                                actionMap.get("viewTypeDetails").actionPerformed(null)

                                                if (showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                                                    return@run
                                                }
                                                if (launcherState.soundfonts.contains(selectedFile.absolutePath)) {
                                                    return@run
                                                }
                                                launcherState.addSoundFont(selectedFile.absolutePath)
                                                soundFonts.add(selectedFile.name)
                                                selectedSoundFont = soundFonts.indexOf(selectedFile.name)
                                            }
                                        },
                                        modifier = Modifier.height(56.dp).width(56.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = i18n.getString("configuration.search_for_soundfont")
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.clickable {
                                    SettingsModal().apply {
                                        isVisible = true
                                    }
                                }
                            ) {
                                Text(text = i18n.getString("settings.settings"), style = MaterialTheme.typography.h6)
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                            }
                            Divider(modifier = Modifier.padding(16.dp).width(width))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Spacer(Modifier.width(16.dp))
                                Button(
                                    onClick = {
                                        playlist = null
                                        if (selectedMIDIFile!!.isDirectory() == true) {
                                            playlist = selectedMIDIFile
                                            beginMidis2jam2PlaylistFwd.invoke()
                                        }
                                        else {
                                            beginMidis2jam2()
                                        }
                                    },
                                    modifier = Modifier.width(120.dp).height(60.dp).padding(0.dp, 0.dp, 0.dp, 16.dp),
                                    enabled = selectedMIDIFile != null && !freeze
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = i18n.getString("start"))
                                        Text(i18n.getString("start"))
                                    }
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = showAbout
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = i18n.getString("about.description"),
                                style = MaterialTheme.typography.h6,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = i18n.getString("about.copyright"),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = Utils.resourceToString("/version.txt"),
                                style = MaterialTheme.typography.body1,
                                modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.clickable {
                                    supportedLocales.let {
                                        val lang =
                                            it[(it.indexOf(it.first { l -> l.language == locale.language }) + 1) % it.size].language
                                        launcherState.setProperty(
                                            "locale",
                                            lang
                                        )
                                        with(Locale.forLanguageTag(lang)) {
                                            Internationalization.locale = this
                                            locale = this
                                        }
                                    }
                                }.padding(0.dp, 0.dp, 0.dp, 0.dp)
                            ) {
                                Image(
                                    painter = painterResource("language.svg"),
                                    contentDescription = "Select locale",
                                    modifier = Modifier.height(32.dp).width(32.dp).padding(8.dp, 4.dp, 4.dp, 4.dp)
                                )
                                Text(
                                    text = locale.language.uppercase(Locale.getDefault()),
                                    style = MaterialTheme.typography.body1,
                                    modifier = Modifier.padding(4.dp, 4.dp, 8.dp, 4.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = i18n.getString("about.warranty"),
                                style = MaterialTheme.typography.body2,
                                textAlign = TextAlign.Center
                            )
                            TextWithLink(
                                MessageFormat.format(
                                    i18n.getString("about.license"),
                                    i18n.getString("about.license_name")
                                ),
                                i18n.getString("about.license_name"),
                                "https://www.gnu.org/licenses/gpl-3.0.en.html",
                                MaterialTheme.typography.body2
                            )
                            Text(
                                text = i18n.getString("about.scott_haag"),
                                style = MaterialTheme.typography.h6.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 0.dp),
                                textAlign = TextAlign.Center
                            )
                            AssetAttribution.loadAttributions().forEach { (name, author, license, url, extra) ->
                                TextWithLink(
                                    MessageFormat.format(
                                        i18n.getString("about.cc_attribution"),
                                        name,
                                        author,
                                        license,
                                        if (extra.isEmpty()) "" else ", ${i18n.getString("about.$extra")}"
                                    ),
                                    license,
                                    url,
                                    MaterialTheme.typography.h6.copy(fontSize = 12.sp)
                                )
                            }
                            Text(
                                text = i18n.getString("about.soundfont_trademark"),
                                style = MaterialTheme.typography.h6.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(0.dp, 16.dp, 0.dp, 16.dp),
                                textAlign = TextAlign.Center
                            )
                            Row(
                                modifier = Modifier.clickable { showOSS = !showOSS },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = i18n.getString("about.oss_licenses"),
                                    style = MaterialTheme.typography.body2
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(ossRotation)
                                )
                            }
                            AnimatedVisibility(
                                visible = showOSS
                            ) {
                                val ossScrollState = ScrollState(0)
                                Column(
                                    modifier = Modifier.width(450.dp).scrollable(ossScrollState, Orientation.Vertical),
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    loadDependencies().forEach { (dependencyName, _, licenses) ->
                                        if (licenses.first().url != null) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.width(width)
                                            ) {
                                                Text(
                                                    text = dependencyName,
                                                    style = MaterialTheme.typography.h6.copy(fontSize = 8.sp)
                                                )
                                                TextWithLink(
                                                    text = licenses.first().name,
                                                    textToLink = licenses.first().name,
                                                    url = licenses.first().url ?: return@Row,
                                                    style = MaterialTheme.typography.h6.copy(fontSize = 8.sp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return LauncherController(
        { setFreeze: Boolean -> freeze = setFreeze },
        { file: File ->
            selectedMIDIFile = file
            midiFileTextField?.invoke(file)
        },
        { freeze }
    )
}

/** Provides a way for the launcher component to allow external modifications. */
data class LauncherController(
    internal val setFreeze: (setFreeze: Boolean) -> Unit,
    internal val setSelectedFile: (file: File) -> Unit,
    internal val getFreeze: () -> Boolean
)

/**
 * The text field that shows the currently selected MIDI file. The field also has a button for opening a file selector.
 */
@Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MIDIFileTextField(
    i18n: ResourceBundle,
    onChangeBySearchButton: (path: File) -> Unit
): (externalFileChange: File) -> Unit {
    var selectedFile: File? by remember { mutableStateOf(null) }
    val externalChange: (File) -> Unit = { externalFileChange ->
        selectedFile = externalFileChange
    }
    TextField(
        value = selectedFile?.name ?: "",
        onValueChange = {},
        singleLine = true,
        modifier = Modifier.padding(0.dp, 0.dp, 0.dp, 16.dp).width(500.dp),
        label = { Text(i18n.getString("configuration.midi_file")) },
        trailingIcon = {
            Row(modifier = Modifier.width(128.dp), horizontalArrangement = Arrangement.End) {
                Icon(
                    Icons.Filled.Face,
                    contentDescription = "Pull from directory",
                    modifier = Modifier.clickable {
                        println("FGSFDS - Assigning directory")
                        /* If the directory is bad, just revert to the home directory */
                        if (!File(launcherState.getProperty("lastdir")).exists()) {
                            launcherState.setProperty("lastdir", USER_HOME_DIRECTORY.absolutePath)
                        }
                        /* Create file chooser modal */
                        var chooser: JFileChooser = JFileChooser(File(launcherState.getProperty("lastdir")))
                        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY)
                        chooser.run {
                            preferredSize = Dimension(800, 600)
                            dialogTitle = "Select MIDI directory"
                            isMultiSelectionEnabled = false
                            actionMap.get("viewTypeDetails").actionPerformed(null) // Switch to "detail" view

                            /* Update path if dialog succeeds */
                            if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                selectedFile = this.selectedFile
                                launcherState.setProperty("lastdir", this.selectedFile.getAbsolutePath())
                                onChangeBySearchButton(selectedFile ?: return@run) // Should be safe
                                logger().info("Selected MIDI directory ${selectedFile?.getAbsolutePath()}")
                            }
                        }
                    }.pointerHoverIcon(PointerIconDefaults.Hand, true)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.List,
                    contentDescription = i18n.getString("midisearch.title"),
                    modifier = Modifier.clickable {
                        MIDISearchFrame.launch()
                    }.pointerHoverIcon(PointerIconDefaults.Hand, true)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Search,
                    contentDescription = i18n.getString("configuration.search_for_midi_file"),
                    modifier = Modifier.clickable {
                        /* If the directory is bad, just revert to the home directory */
                        if (!File(launcherState.getProperty("lastdir")).exists()) {
                            launcherState.setProperty("lastdir", USER_HOME_DIRECTORY.absolutePath)
                        }

                        /* Create file chooser modal */
                        JFileChooser(File(launcherState.getProperty("lastdir"))).run {
                            fileFilter = object : FileFilter() {
                                override fun accept(file: File?): Boolean {
                                    return file?.extension?.lowercase(Locale.getDefault()) == "mid" || file?.extension?.lowercase(
                                        Locale.getDefault()
                                    ) == "kar" || file?.extension?.lowercase(Locale.getDefault()) == "midi" || file?.isDirectory == true
                                }

                                override fun getDescription(): String = "MIDI files (*.mid, *.midi, *.kar)"
                            }
                            preferredSize = Dimension(800, 600)
                            dialogTitle = "Select MIDI file"
                            isMultiSelectionEnabled = false
                            actionMap.get("viewTypeDetails").actionPerformed(null) // Switch to "detail" view

                            /* Update path if dialog succeeds */
                            if (showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                selectedFile = this.selectedFile
                                launcherState.setProperty("lastdir", this.selectedFile.parent)
                                onChangeBySearchButton(selectedFile ?: return@run) // Should be safe
                                logger().info("Selected MIDI file ${selectedFile?.absoluteFile}")
                            }
                        }
                    }.pointerHoverIcon(PointerIconDefaults.Hand, true)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        },
        readOnly = true
    )
    return externalChange
}

/**
 * [Properties] that has some extra functionality useful for the launcher.
 */
class LauncherProperties(existing: Properties) : Properties(existing) {
    /**
     * Sets a property.
     */
    override fun setProperty(key: String, value: String): Any? {
        super.setProperty(key, value).also {
            store(FileWriter(LAUNCHER_STATE_FILE), null)
            return it
        }
    }

    /**
     * Adds a SoundFont to the launcher state, given its [path].
     */
    fun addSoundFont(path: String) {
        this.setProperty("soundfonts", Json.encodeToString(soundfonts.apply { add(path) }))
    }

    /**
     * Returns a [File] corresponding to the [name] of a SoundFont previously registered in this object.
     */
    fun getSoundFontFile(name: String): File = File(soundfonts.first { File(it).name == name })

    /** List of paths of SoundFonts. */
    val soundfonts: ArrayList<String>
        get() = Json.decodeFromString<MutableList<String>>(getProperty("soundfonts")) as ArrayList<String>
}
