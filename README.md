# YouTube Video Downloader (Android)

**Русский | [English](#english)**

Приложение для сохранения видео и аудио с YouTube на Android, в том числе отдельных роликов из плейлистов.

## Возможности

- Скачивание видео в поддерживаемых форматах и разрешениях; сохранение аудио в MP3 или M4A.
- Предпросмотр названия, длительности и доступных вариантов качества.
- Плейлисты: просмотр списка, отметки отдельных видео и выбор качества для каждого ролика.
- Очередь загрузок с прогрессом, паузой, продолжением, повтором и удалением.
- Выбор папки, открытие скачанного файла и папки с ним. По желанию плейлисты сохраняются в собственных подпапках выбранной папки загрузок.
- Светлая, тёмная и системная темы.
- English / Русский в настройках. На новой установке выбран English.
- Проверка обновлений приложения и компонента работы с YouTube.

## Установка и обновление

Скачайте APK на [странице релизов](https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases/latest) и установите его на Android. Официальные APK подписаны постоянным ключом; обновление выполняется поверх предыдущей официальной версии. Если установлен старый тестовый APK с другой подписью, сначала удалите его (сохранив нужные данные).

## Использование

1. Вставьте ссылку на видео или плейлист YouTube.
2. Получите информацию, выберите формат и качество.
3. Нажмите «Скачать без очереди» или «Добавить в очередь».
4. Следите за загрузками на вкладке «Очередь».

Для плейлиста можно выбрать отдельные записи и настроить качество каждого видео. Доступность форматов и разрешений зависит от источника.

## Изменения

### v0.5.12
- Исправлена отрисовка скруглённых всплывающих списков при прокрутке: общее окно и его содержимое обрезаются по одному округлому контуру. Проверены компоненты выбора качества, фильтров, настроек и меню очереди.
- Индивидуальный раскрытый список: сначала все качества **видео MP4** (от «Максимальное» до минимального), затем разделитель и все качества **аудио MP3** (от «Максимальное» и 320 кбит/с до 128 кбит/с). Пункты имеют полные названия с единицами; компактная строка остаётся прежней. При наличии только одной группы разделитель не показывается.
- Реальные качества YouTube продолжают обновляться в фоне, но открытый список не перестраивается и не сбрасывает выбор. При открытии меню выбранный пункт автоматически показывается в видимой области.
- Надпись фоновой проверки качеств фиксирована в три строки («Определено» / «качеств:» / «N из M»). Кнопка остановки/повтора — с переносом на две строки и вертикальным центрированием, без наложения на счётчик.
- Кнопка «Скачать без очереди» показывает количество выбранных файлов: «Скачать без очереди (N)», как обычное добавление в очередь.
- Закреплена обрезка верхней информационной карточки и её содержимого по скруглённым краям, включая движение при резкой прокрутке и эффекте резинки. Фон и обводка списка учитывают смещение верхнего overscroll; эффект резинки сохранён.
- Сохранены индивидуальные настройки, согласованные цвета 050, затемнение рамок светлой темы и финальная иконка A. Весь новый текст доступен на русском и английском языках.

### v0.5.11
- Плейлист: два варианта добавления отмеченных файлов — обычная очередь и «Скачать без очереди»; индивидуальные MP4/MP3 и качество сохраняются по ID видео при повторном открытии и перестановке.
- Общий диалог «Формат и качество для отмеченных» переключает видео MP4 (Максимальное, 4K–360p) и аудио MP3 (Максимальное, 128/192/256/320 кбит/с). В строках компактные подписи MP4 · Макс., MP4 · 1080p, MP3 · Макс., MP3 · 192k; русская и английская локализация.
- Настройки → Плейлисты: добровольное определение реальных качеств YouTube для каждого видео; по умолчанию отключено, с предупреждением о длительности. До получения сведений доступны предустановленные варианты, возле поля отображаются часы/спинер/галочка/предупреждение. Проверка видимых и отмеченных видео приоритетна, полученные сведения кэшируются. Проверку можно остановить и возобновить.
- На странице «Загрузка» после определения плейлиста скрываются «Параметры загрузки» и «Скачивание»; одиночные видео работают как прежде. Прокси-поле отключается вместе с чекбоксом, сохраняя значение.
- На странице плейлиста индикатор прокрутки появляется только вместе с верхней стрелкой и рисуется ниже закреплённых фильтров. При эффекте резинки фон/контур синхронизирован с фактическим положением информационного блока. Исходный эффект резинки сохранён.
- Индивидуальное поле качества: 9/2 dp и ширина +1 dp. Общие рамки светлой темы усилены на 15% (#B3B8C1); тёмная тема не меняется.
- Ограничение разрешения видео без масштабирования вверх. Для MP3 при скачивании определяется исходный аудиобитрейт и выбирается разумное ограничение кодирования (при недоступности сведений — консервативный предел 192 кбит/с); фактическое качество скачанных файлов отображается, когда оно известно.
- Утверждённая иконка A и прежняя цветовая палитра сохранены.

### v0.5.10
- Восстановлен утверждённый внешний вид страницы плейлиста из v0.5.8 без возврата старого механизма прокрутки. Информационная карточка прокручивается естественно, а «Видео в плейлисте», счётчик, чекбокс и кнопки качества/фильтра закрепляются под верхней панелью.
- Убраны двойные скруглённые рамки и торчащие прямоугольные углы вложенных блоков: одна общая карточка списка с прежними радиусом 14 dp, отступом 8 dp и корректным обрезанием содержимого. Полоса прокрутки расположена справа.
- Поле индивидуального качества: от текста до левой рамки 8 dp, от текста до зоны стрелки 2 dp (смещение текста на 1 dp вправо без изменения ширины поля).
- В диалоге «Качество для отмеченных» расстояние между заголовком и выпадающим списком увеличено на 3 dp относительно v0.5.9; отступы до нижних кнопок не менялись.
- Фиксированный список разрешений, мгновенная доступность выбора, компактные уведомления и усиленное выделение строк из v0.5.9 сохранены; палитра и утверждённая иконка A не менялись.

### v0.5.9
- Плейлист прокручивается непрерывно, как главная страница: обложка и описание уходят обычным движением, а «Видео в плейлисте», счётчик, «Выбрать всё», массовое качество и фильтр закрепляются под верхней панелью только после достижения её края. Убраны искусственное сворачивание и перехват касаний; стрелки и восстановление позиции адаптированы.
- Возвращён быстрый фиксированный выбор индивидуального качества: «Максимальное», 2160p, 1440p, 1080p, 720p, 480p и 360p. Отключены фоновые проверки разрешений каждого видео и связанные с ними спиннеры/блокировки чекбоксов; массовое качество сохранено.
- Компактное поле качества: 7 dp слева от текста и 3 dp справа до зоны стрелки при неизменной ширине.
- Выделение активной строки: 31% в тёмной теме, 60% в светлой (цвет #E6E7EB). Палитра остальных элементов и иконка A не менялись.
- Полосы «Запрос обновления информации отменён» на страницах «Загрузка» и «Плейлист» снова тонкие, как в v0.5.7; время отображения — 15 секунд.

### v0.5.8
- Плейлисты: карточка с обложкой и описанием уходит вверх при прокрутке; заголовок списка и кнопка добавления остаются закреплёнными. Стрелки обеспечивают прокрутку на экран, удержание — к краю.
- Для каждого видео в фоне определяются реальные доступные разрешения, сохраняемые для повторных открытий. Пока идёт проверка, выбор качества и чекбокс заблокированы, отображается индикатор 20 dp; при ошибке доступен прежний список качеств.
- Фильтры и массовое качество вынесены в отдельные компактные кнопки 42×38 dp. Сброс фильтров также снимает «Сначала отмеченные»; массовое качество учитывает доступные разрешения каждого видео.
- Отмена обновления показывает сообщение на 15 секунд с плавным появлением и исчезновением; улучшено переключение вкладок, контраст выделения, ширина бейджей и интервалы.
- Красные элементы управления используют акцент #FE1F2C, при этом утверждённая палитра «Цвета 050», градиент кнопок и оригинальная иконка A не изменены.

### v0.5.7
- Плейлист: исправлены обрезанные названия индивидуального качества, массовая смена качества, восстановление позиции при фильтрации и снятие чекбоксов после добавления в очередь.
- Подсветка выбранной строки теперь на всю ширину и немного заметнее; добавлены плавающие кнопки прокрутки как в очереди, исправлен свайп назад.
- Обновление информации о плейлисте не открывает другие экраны: 5-секундные уведомления об успехе и ошибке, при отмене статус остаётся внутри блока. Кнопки «Отмена» унифицированы.
- Очередь: верхняя стрелка ориентируется на заголовок первой группы, убран знак приблизительности в процентах, улучшена заметность уведомления о добавлении.
- Сокращён межкнопочный отступ «Обновить информацию» / «Открыть список видео». Иконка A и палитра 050 сохранены.

### v0.5.6
- Исправлены потерянные миниатюры и прокрутка больших плейлистов, восстановление точной позиции и автоматическое движение выбранного длинного названия.
- В плейлисте: мягкая подсветка выбранной строки, свайп назад, компактные поля качества 20 dp с полным текстом и выравниванием, настройки фильтров без лишней иконки.
- Очередь: контурные стрелки 78×38 dp, плавное появление за 300 мс, точные границы показа и полная прокрутка до краёв страницы.
- Убраны ореолы чекбоксов и подёргивание закреплённых вкладок.
- Обновление плейлиста теперь отображается внутри карточки; отмена показывает статус на месте индикатора. Уточнены кнопки, отступы, отображение пути и прогресс видео/аудио.

### v0.5.5
- Общий прогресс файла и отдельный прогресс видео/аудио; исправлены скачки процентов и несоответствие полосы прогресса.
- Очередь: выделение записей, бегущие названия, кнопки прокрутки на экран и до края по удержанию, сохранение позиции при переключении страниц.
- Обновление информации о завершённых файлах из локального хранилища: для отдельного файла и массовое с прогрессом и отменой. Исправлены статусы удалённых файлов и очистка временных загрузок.
- Плейлисты: новые списки открываются без отметок, сохраняются выбранные качества и положение прокрутки, статусы синхронизируются с реальной очередью. Быстрый переход в начало очереди из уведомления о добавлении.
- Унифицированы кнопки, поля и элементы списков (46 dp), кроме верхних вкладок и индивидуального поля качества (18 dp). Качество ниже 360p больше не предлагается.
- Компактные индивидуальные поля качества, выравнивание по длительности, обновлённые фильтры и сброс, динамические стрелки выпадающих меню, согласованные отступы и оформление настроек.

### v0.5.4
- Установлена окончательно утверждённая иконка _A_192px из Icon Lab v0.5.11: оригинальный PNG без adaptive-обработки и подложек, с проверкой SHA-256 в сборке.
- Индивидуальный выбор качества переведён на стандартный Spinner приложения: 28 dp, динамическая ширина, длительность и качество на одной строке, никаких галочек в меню.
- Добавлены подпапки с названиями плейлистов непосредственно в основной папке загрузки, с повторным использованием прежнего имени. Настройка включена на новых установках; при обновлении сохраняется старое поведение до явного включения.
- Исправлены значки RU/EN, отступы подписей чекбоксов, иконки папок и выравнивание заголовка списка.
- Для скачанного файла без известного разрешения выводится «Качество неизвестно».

### v0.5.3
- Исправлены сохранение отметок видео при изменении порядка плейлиста и повторные запросы к YouTube.
- Улучшены уведомления о добавлении, прогресс и сообщения об ошибках.
- Переработаны чекбоксы, выбор качества, фильтры и отступы; уточнены статусы файлов.
- Изменены размеры и ресурсы иконки для более крупного отображения без белой подложки.

### v0.5.2
- Английский и русский интерфейс с выбором языка.
- Компактный список плейлиста, фильтры и статусы скачивания.
- Обновление плейлиста на обеих страницах.
- Восстановлены цвета оформления 050 и обновлена иконка.

### v0.5.1
- Переработан экран плейлиста: компактный выбор видео и качества.
- Улучшены обновление списка и сохранение настроек отдельных видео.
- Обновлены оформление и иконка приложения.

### v0.5.0
- Улучшены очередь, просмотр файлов и загрузка плейлистов.
- Добавлены более удобные списки и подтверждения удаления.

Остальные версии и полная история публикаций доступны в [GitHub Releases](https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases).

## Примечания

Для работы необходим интернет. Некоторые ролики могут быть недоступны из-за ограничений источника. Скачивайте только материалы, на которые у вас есть права или разрешение. Используется проект [yt-dlp](https://github.com/yt-dlp/yt-dlp).

## Лицензия

См. [LICENSE](LICENSE).

---

# English

**[Русский](#youtube-video-downloader-android) | English**

An Android app for downloading YouTube videos and audio, including selected videos from playlists.

## Features

- Download supported video resolutions and formats; save audio as MP3 or M4A.
- Preview titles, durations, and available quality options.
- Browse playlists, select individual videos, and set a separate resolution for each one.
- Download queue with progress, pause, resume, retry, and removal.
- Choose a save folder and open downloaded files or their folders. Optionally save each playlist into its own subfolder under the selected download folder.
- Light, dark, and system themes.
- English / Русский language selection in Settings. English is the default on fresh installations.
- Check for app updates and updates to the YouTube download component.

## Install and update

Download the APK from [GitHub Releases](https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases/latest) and install it on Android. Official APKs use the same signing key, allowing updates over earlier official versions. If an old test build used a different signature, uninstall it first after backing up any important data.

## Getting started

1. Paste a YouTube video or playlist link.
2. Fetch information and select the format and quality.
3. Choose “Download immediately” or “Add to queue”.
4. Track downloads on the Queue tab.

Playlists let you choose individual entries and set per-video quality. Available formats and resolutions depend on the source.

## What's changed

### v0.5.12
- Correctly clip scrolling dropdown entries to the one shared rounded popup outline. This applies to individual quality, bulk controls, filters, settings, and the queue action menu.
- Per-video expanded menu lists all **MP4 video** choices in descending quality order (Maximum first), followed by a nonselectable separator and all **MP3 audio** choices descending from Maximum/320 kbps to 128 kbps. Use full names and units inside the popup while keeping compact field labels. Hide the divider when only one media group is available.
- Background YouTube quality discovery never rebuilds an open popup or loses a chosen quality; opening a popup scrolls to the current selection.
- Quality verification counter uses three deliberate lines, and the centered Stop/Retry button uses two lines without text overlap.
- The urgent action now includes the selected file count: "Download immediately (N)".
- Constrain playlist header children to its true rounded outline and synchronize list background/border with top overscroll displacement while retaining native stretch behavior.
- Preserve approved v0.5.11 media controls, color palette, dark theme and final Icon A. RU/EN localization remains supported.

### v0.5.11
- Playlist: normal and priority queue actions for selected items; each entry has independent MP4/MP3 output and quality, saved by stable video ID across revisits and reorderings.
- One “Format and quality for selected” dialog chooses MP4 video (Maximum, 4K–360p) or MP3 audio (Maximum, 128/192/256/320 kbps). Compact per-row labels show MP4 · Max, MP4 · 1080p, MP3 · Max or MP3 · 192k in English and Russian.
- Settings → Playlists: opt-in, potentially very slow per-video YouTube quality verification. Preset choices remain usable while loading; spinner, clock, check or warning indicates the state. Selected/visible entries take priority; results are cached. Scan can be stopped or restarted.
- On Downloads, after a playlist is identified, hide the Download options and Downloading panels. Single-video controls remain. Disable the proxy address input whenever its checkbox is off, retaining the address.
- Playlist scroll thumb is shown only with the up arrow and strictly below pinned filters; align card drawing with its changing header during overscroll. Retain Android stretch behavior.
- Approved individual quality insets 9/2 dp with 1dp wider field; darken shared light-theme borders by 15% (#B3B8C1) without changing dark colors.
- No video upscaling: selected resolution acts as a ceiling. MP3 output bitrate uses source information where available at download time (conservative 192 kbps ceiling if unavailable); show observed media quality when recorded.
- Approved Icon A and the established palette are unchanged.

### v0.5.10
- Restored the original v0.5.8 playlist appearance without reverting the new continuous scrolling: the playlist summary scrolls naturally and list controls pin below the fixed toolbar.
- Removed doubled rounded borders and protruding rectangular corners, using one shared card surface and original 14dp corner radius and 8dp top inset, clipping list items correctly. Scrollbar remains at the right edge.
- Individual quality text now has 8dp on the left and 2dp before the arrow area, moving text 1dp to the right with unchanged field width.
- Added exactly 3dp above the bulk-quality dropdown compared with v0.5.9; spacing below it remains unchanged.
- Retained instant preset quality options, compact refresh cancellation notices, approved theme contrast and launcher Icon A.

### v0.5.9
- Playlist cover and description scroll away normally; list heading, selection count, Select all, bulk quality and filters become sticky only when they reach the fixed toolbar. One continuous scroll replaces the collapsing cover and touch interception, preserving navigation arrows and scroll position.
- Restored immediate, fixed per-video quality choices (Best, 2160p, 1440p, 1080p, 720p, 480p, 360p). Removed per-video YouTube resolution probes, waiting indicators and blocked checkboxes; bulk quality remains available.
- Compact quality label padding is now 7 dp from the left edge and 3 dp to the arrow area without increasing field width.
- Active row highlight strength: 31% in dark mode and 60% in light mode (tint #E6E7EB). Other palette colors and approved Icon A are unchanged.
- Cancellation notices on Downloads and Playlist return to the slim v0.5.7 layout, keeping the 15-second lifetime.

### v0.5.8
- Playlist cover and description slide away while scrolling; the list heading and Add to queue button stay pinned. Floating arrows move one screen per tap or to the edge when held.
- Actual supported video resolutions are discovered in the background and cached. Quality and selection controls stay disabled with a 20 dp indicator until discovery finishes; the standard quality list remains available on failure.
- Filters and bulk quality now have separate compact 42×38 dp buttons. Reset also clears Selected first; bulk quality respects each video's available resolutions.
- Cancelled refresh status appears for 15 seconds with smooth expansion and retraction. Improved tab switching, row selection contrast, badge widths and spacing.
- Accent red uses #FE1F2C while Colors 050, the primary-button gradient, and the original approved A launcher icon are preserved.

### v0.5.7
- Fixed cropped per-video quality labels, bulk quality changes and filtered scroll restoration; checkboxes reset after successful enqueue while qualities persist.
- Full-width stronger playlist selection band, queue-style floating scroll arrows and better guarded swipe-back.
- Playlist refresh no longer navigates: five-second success/error overlays; cancellation remains in the status panel and Cancel button matches Download.
- Queue uses first group header for the upper arrow, removes approximate progress glyph, and improves enqueue notice visibility.
- Standardized spacing between Refresh information and Open video list; approved icon A and Colors 050 retained.

### v0.5.6
- Fixed thumbnail requests and precise playlist scroll restoration; selected titles automatically marquee without manual dragging.
- Playlist selection highlighting, protected back swipe, 20 dp quality pickers, filter reset layout, and touch halo removal.
- Compact outline queue navigation with 300 ms fade, exact boundary conditions, and smooth edge scrolling.
- Fixed pinned tab jitter, progress descriptions for video versus audio, storage path display, and playlist metadata refresh/cancellation panel.

### v0.5.5
- Distinct overall-file and video/audio-stream progress, with fixes for percentage jumps and progress-bar mismatch.
- Queue row selection, marquee titles, page-sized arrow scrolling and long-press jumps, and retained scroll position when switching tabs.
- Local-only metadata refresh for an individual completed file or all completed files, with progress and cancellation; safer partial-download cleanup and missing-file statuses.
- Playlists start unchecked, preserve chosen qualities and scroll position, and reflect actual queue state. Added a quick jump to the top of the queue after enqueueing.
- Unified regular button, field and menu-item heights to 46 dp (top tabs and the 18 dp per-video quality field remain exceptions). Minimum offered quality is 360p.
- Refined per-video quality sizing and alignment, filters and reset action, rotating dropdown arrows, and Settings spacing and styling.

### v0.5.4
- Adopted the user-approved original _A_192px PNG launcher from Icon Lab v0.5.11, with no adaptive icon or background and an SHA-256 build guard.
- Per-video quality uses the existing Spinner, a 28 dp tall dynamic-width field and a single row alongside duration; removed nonstandard popup check marks.
- Optional stable, named playlist subfolders directly inside the chosen download directory. Enabled for fresh installs; existing users keep their previous save behavior until they enable it.
- Corrected RU/EN selector icons, checkbox label spacing, folder button icons and playlist header baseline alignment.
- Completed downloads with no measured resolution now display “Quality unknown”.

### v0.5.3
- Kept playlist selections tied to video IDs and unified playlist metadata requests.
- Improved queue confirmations, progress reporting, and user-facing errors.
- Refined checkboxes, quality menus, filters, spacing, and file status labels.
- Enlarged the launcher artwork and switched to adaptive transparent resources.

### v0.5.2
- English and Russian UI with a language setting.
- Compact playlist browser, filters, and download status labels.
- Playlist refresh across screens.
- Restored the 050 color palette and refined the launcher icon.

### v0.5.1
- Redesigned playlist screen with a more compact selection and quality interface.
- Improved playlist refresh and remembered per-video quality settings.
- Updated visual style and launcher icon.

### v0.5.0
- Improved download queue, file opening, and playlist handling.
- More convenient menus and safe deletion confirmations.

See [GitHub Releases](https://github.com/lKlElNlGl/YoutubeVideoDownloader/releases) for all published versions.

## Notes

Internet access is required. Some videos may be unavailable due to source restrictions. Only download content you have permission to use. The app relies on [yt-dlp](https://github.com/yt-dlp/yt-dlp).

## License

See [LICENSE](LICENSE).
