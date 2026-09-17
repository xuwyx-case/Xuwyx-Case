extends Control

const MAIN_MENU_PATH := "res://scenes/menu/main_menu.tscn"
const MINIMUM_DISPLAY_SECONDS := 1.15
const POLL_INTERVAL_SECONDS := 0.05

@onready var progress_bar: ProgressBar = %ProgressBar
@onready var status_label: Label = %StatusLabel

var _minimum_time_elapsed := false
var _menu_loaded := false
var _transition_started := false


func _ready() -> void:
	# No per-frame processing: short-lived timers poll only while the first scene loads.
	set_process(false)
	ResourceLoader.load_threaded_request(MAIN_MENU_PATH, "PackedScene", true)
	_start_minimum_display_timer()
	_poll_loading_status()


func _start_minimum_display_timer() -> void:
	await get_tree().create_timer(MINIMUM_DISPLAY_SECONDS, true, false, true).timeout
	_minimum_time_elapsed = true
	_try_open_menu()


func _poll_loading_status() -> void:
	var progress: Array = []
	var status := ResourceLoader.load_threaded_get_status(MAIN_MENU_PATH, progress)

	match status:
		ResourceLoader.THREAD_LOAD_IN_PROGRESS:
			var value := 0.0
			if not progress.is_empty():
				value = clampf(float(progress[0]) * 100.0, 0.0, 96.0)
			progress_bar.value = maxf(progress_bar.value, value)
			await get_tree().create_timer(POLL_INTERVAL_SECONDS, true, false, true).timeout
			_poll_loading_status()
		ResourceLoader.THREAD_LOAD_LOADED:
			_menu_loaded = true
			progress_bar.value = 100.0
			status_label.text = "ГОТОВО"
			_try_open_menu()
		_:
			status_label.text = "ОШИБКА ЗАГРУЗКИ"
			push_error("Failed to load main menu scene: %s" % MAIN_MENU_PATH)


func _try_open_menu() -> void:
	if _transition_started or not _minimum_time_elapsed or not _menu_loaded:
		return

	_transition_started = true
	var packed_scene := ResourceLoader.load_threaded_get(MAIN_MENU_PATH) as PackedScene
	if packed_scene == null:
		status_label.text = "ОШИБКА ЗАГРУЗКИ"
		push_error("Loaded main menu resource is not a PackedScene")
		return

	get_tree().change_scene_to_packed(packed_scene)

