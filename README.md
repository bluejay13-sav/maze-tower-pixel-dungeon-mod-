# Maze Tower (미궁의 마탑)

**Maze Tower is an unofficial fan mod of [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/).**
It is not made, affiliated with, or endorsed by Shattered Pixel (Evan Debenham).
Please do not report Maze Tower bugs to the Shattered Pixel Dungeon developers.

Shattered Pixel Dungeon is an open-source roguelike by Evan Debenham, based on the
[source code of Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle) by [Watabou](https://watabou.itch.io/).
The original game's code is at [00-Evan/shattered-pixel-dungeon](https://github.com/00-Evan/shattered-pixel-dungeon).
If you enjoy it, please consider buying or [supporting](https://www.patreon.com/ShatteredPixel) the original game.

## What is different

You climb a tower instead of descending a dungeon. Maze Tower adds, among other things:
- a mana resource and weapon skills for swords, daggers, bows and the mage's staff
- a four-stage class progression (Sprout → Bloom → Full Bloom → Fruit) with new talents
- elemental reactions (steam, explosion, thaw, freeze) and new status effects
- new bosses, new region art, and two endings

Text is available in Korean and English.

## Boss Trial (test build)

The Boss Trial release lets you try three boss fights right away, as the Warrior:
- Floor 5: Rotting Sap Mass (start on floor 4, level 6)
- Floor 20: Wraith King (start on floor 19, level 19)
- Floor 20: Mage-City Empress (start on floor 19, level 19)

You start with every item from the floors before the boss. Climb the stairs up to reach the boss room.

**How to play (Windows):** download the zip from the Releases page, unzip it anywhere, and run `Maze Tower.exe`.
No separate Java install is needed.

The Boss Trial is built from the `boss-trial` branch of this repository (the only change from `main` is `MazeTower.BOSS_TRIAL = true`).

## Building from source

Follow the original [desktop compile guide](docs/getting-started-desktop.md):
- run: `gradlew desktop:debug`
- runnable jar: `gradlew desktop:release` (output in `desktop/build/libs`)
- Windows app folder with a bundled Java runtime: `gradlew desktop:jpackageImage` (output in `desktop/build/jpackage`)

## License

Maze Tower, like Shattered Pixel Dungeon and Pixel Dungeon, is licensed under the
**GNU General Public License v3.0** — see [LICENSE.txt](LICENSE.txt).
All original credits are kept in the in-game About screen. The full source code of every Maze Tower release is in this repository.

---

# 미궁의 마탑 (한국어)

**미궁의 마탑은 [Shattered Pixel Dungeon](https://shatteredpixel.com/shatteredpd/)(녹픽던)을 바탕으로 만든 비공식 팬 모드입니다.**
원작 개발자(Shattered Pixel, Evan Debenham)가 만들거나 승인한 게임이 아닙니다. 미궁의 마탑의 버그를 원작 개발자에게 알리지 말아 주세요.

## 보스전 체험판

전사로 보스전 세 가지를 바로 해 볼 수 있는 시험 버전입니다.
- 5층 썩은 수액 덩어리 (4층·레벨 6에서 시작)
- 20층 망령 왕 (19층·레벨 19에서 시작)
- 20층 마도시의 여제 (19층·레벨 19에서 시작)

보스층 바로 앞 층까지의 아이템을 모두 받은 채 시작하며, 올라가는 계단으로 올라가면 보스방입니다.

**실행 방법 (Windows):** Releases 페이지에서 zip을 받아 아무 곳에나 압축을 풀고 `Maze Tower.exe`를 실행하세요. 자바를 따로 설치하지 않아도 됩니다.

## 라이선스

원작과 같은 **GNU GPL v3.0** 라이선스를 따릅니다([LICENSE.txt](LICENSE.txt)). 원작 크레딧은 게임 안 크레딧 화면에 그대로 있으며, 배포한 모든 버전의 소스 코드를 이 저장소에 공개합니다.
