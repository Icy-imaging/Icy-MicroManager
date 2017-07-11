/*
 * Copyright 2010-2015 Institut Pasteur.
 * 
 * This file is part of Icy.
 * 
 * Icy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Icy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Icy. If not, see <http://www.gnu.org/licenses/>.
 */
// LegacyInjector.java
//

/*
 * ImageJ software for multidimensional image processing and analysis.
 * 
 * Copyright (c) 2010, ImageJDev.org.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the names of the ImageJDev.org developers nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package plugins.tprovoost.Microscopy.MicroManager.patch;

import icy.plugin.PluginLoader;
import icy.system.ClassPatcher;

/**
 * Overrides class behavior of Micro-Manager classes using bytecode manipulation. This
 * class uses the {@link ClassPatcher} (which uses Javassist) to inject method
 * hooks, which are implemented in the {@link plugins.tprovoost.Microscopy.MicroManager.patch}
 * package.
 * 
 * @author Stephane Dallongeville
 */
public class MMPatcher
{
    private static final String PATCH_PKG = "plugins.tprovoost.Microscopy.MicroManager.patch";
    private static final String PATCH_SUFFIX = "Methods";

    /** Overrides class behavior of Micro-Manager classes by injecting method hooks. */
    public static void applyPatches()
    {
        final ClassLoader classLoader = PluginLoader.getLoader();
        final ClassPatcher hacker = new ClassPatcher(classLoader, PATCH_PKG, PATCH_SUFFIX);

        // need to load it first

        // override behavior of org.micromanager.MMStudio
        hacker.replaceMethod("org.micromanager.MMStudio", "public boolean isLiveModeOn()");
        hacker.replaceMethod("org.micromanager.MMStudio", "public void enableLiveMode(boolean enable)");

        // this directly load the new patched MMStudio class in the Plugin class loader
        hacker.loadClass("org.micromanager.MMStudio", classLoader, null);
        
//        ici il faudrait faire en sorte que que le pluginloader ajoute la class dans la liste des chargée
//        sinon on a une erreur de duplication (bidouiller le JarClassLoader pour autoriser ça peut être)
        
        // hacker.loadClass("org.micromanager.internalinterfaces.LiveModeListener");
        // hacker.loadClass("org.micromanager.utils.MMFrame");
        // hacker.loadClass("org.micromanager.MainFrame");
        // hacker.loadClass("org.micromanager.utils.MMScriptException");
        // hacker.loadClass("org.micromanager.MMStudio$2");
        // hacker.loadClass("org.micromanager.MMStudio$DisplayImageRoutine");
        // hacker.loadClass("org.micromanager.utils.MMDialog");
        // hacker.loadClass("org.micromanager.positionlist.PositionListDlg");
        // hacker.loadClass("org.micromanager.conf2.MMConfigFileException");
        // hacker.loadClass("org.json.JSONException");
        // hacker.loadClass("org.micromanager.utils.MMException");
        // hacker.loadClass("org.micromanager.graph.GraphFrame");
    }
}
