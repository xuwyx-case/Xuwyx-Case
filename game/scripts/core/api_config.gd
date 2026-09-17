class_name ApiConfig
extends RefCounted

## Single source of truth for every future backend request.
const API_BASE_URL := "https://api.xuwyxgames.com"


static func endpoint(path: String) -> String:
	var normalized_path := path.strip_edges()
	if normalized_path.is_empty():
		return API_BASE_URL
	if not normalized_path.begins_with("/"):
		normalized_path = "/" + normalized_path
	return API_BASE_URL + normalized_path

