package com.sync2pc.app

import com.sync2pc.app.mirror.LocalTree
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalTreeLabelTest {
    @Test fun downloadsPickerPathsAreShortened() {
        assertEquals("Download/Test", LocalTree.rawPathLabel("/storage/emulated/0/Download/Test"))
        assertEquals("Download/Test", LocalTree.rawPathLabel("/storage/emulated/0/Download/Test/"))
        assertEquals("(Hauptordner)", LocalTree.rawPathLabel("/storage/emulated/0/"))
        assertEquals("SD-Karte: DCIM", LocalTree.rawPathLabel("/storage/1234-ABCD/DCIM"))
    }
}
