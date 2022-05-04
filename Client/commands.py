from enum import Enum


class Commands(str, Enum):
    MOVE = "MOVE"
    INTERACT_WITH_FOOD_BOX = "INTERACT_WITH_FOOD_BOX"
    CONNECTION_REQUEST = "CONNECTION_REQUEST"
    DISCONNECTED = "DISCONNECTED"
    STALE = "STALE"
    GAME_STARTED = "GAME_STARTED"
