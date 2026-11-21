/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dobrokhotov
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.JNI;
import org.lwjgl.system.windows.WindowsLibrary;

//Maze Tower: 윈도우에서 한글 입력기가 켜져(한글 상태) 있으면 키 입력이 입력기로 넘어가 게임 조작이 먹지 않는다.
//평소에는 게임 창의 입력기를 꺼서 한/영 상태와 상관없이 같은 키로 조작되게 하고,
//글 입력 칸(이름·시드 등)이 열려 있는 동안에만 다시 켜서 한글을 쓸 수 있게 한다.
public class DesktopIme {

	private static final int IACE_DEFAULT = 0x0010;

	private static boolean failed = false;
	private static long fnAssociate = 0;
	private static long window = 0;
	private static boolean wanted = false;

	public static void setWindow( long glfwWindow ){
		window = glfwWindow;
		apply();
	}

	//true = 글 입력 중이라 입력기를 켬, false = 게임 조작 중이라 끔
	public static void setEnabled( boolean value ){
		wanted = value;
		apply();
	}

	public static void apply(){
		if (failed || !System.getProperty("os.name", "").startsWith("Windows")) return;
		try {
			long glfw = window;
			if (glfw == 0 && Gdx.graphics instanceof Lwjgl3Graphics){
				glfw = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
			}
			if (glfw == 0) return;
			long hwnd = GLFWNativeWin32.glfwGetWin32Window(glfw);
			if (hwnd == 0) return;
			if (fnAssociate == 0){
				WindowsLibrary imm = new WindowsLibrary("imm32.dll");
				fnAssociate = imm.getFunctionAddress("ImmAssociateContextEx");
				if (fnAssociate == 0){
					failed = true;
					return;
				}
			}
			//BOOL ImmAssociateContextEx(HWND, HIMC, DWORD): HIMC가 NULL이고 flags 0이면 입력기 끄기, IACE_DEFAULT면 원래대로
			JNI.callPPI(hwnd, 0L, wanted ? IACE_DEFAULT : 0, fnAssociate);
		} catch (Throwable t){
			failed = true;
		}
	}
}
