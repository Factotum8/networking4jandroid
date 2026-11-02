package com.factotum8.networking4jandroid.ui.common

import com.factotum8.networking4jandroid.data.local.entity.enums.ActionType
import com.factotum8.networking4jandroid.data.local.entity.enums.Circle
import com.factotum8.networking4jandroid.data.local.entity.enums.ContactType
import com.factotum8.networking4jandroid.data.local.entity.enums.DimensionLinkType
import com.factotum8.networking4jandroid.data.local.entity.enums.InteractionType

/**
 * Human-readable display labels for enum values, shared across screens.
 *
 * `Circle`/`ContactType` keep their exact spec-mandated English form (full
 * names only, never the "SC" abbreviation) — named concepts from the book
 * the app is based on, not generic words, so they aren't translated.
 * `ActionType`/`InteractionType`/`DimensionLinkType` get Russian labels for
 * UI chrome, matching this project's sessions.
 *
 * Ported from `reference/ui/labels.py`.
 */
val CIRCLE_LABELS: Map<Circle, String> = mapOf(
    Circle.SUPPORT_CIRCLE to "Support Circle",
    Circle.FUNCTIONAL_CIRCLES to "Functional Circles",
    Circle.SUCCESS_CIRCLE to "Success Circle",
)

val CONTACT_TYPE_LABELS: Map<ContactType, String> = mapOf(
    ContactType.CONNECTOR to "Connector",
    ContactType.CONDENSER to "Condenser",
    ContactType.BRIDGE to "Bridge",
    ContactType.INSIDER to "Insider",
)

val ACTION_TYPE_LABELS: Map<ActionType, String> = mapOf(
    ActionType.WRITE to "Написать",
    ActionType.CALL to "Позвонить",
    ActionType.MEET to "Встретиться",
    ActionType.FOLLOW_UP_LATER to "Вернуться позже",
)

val INTERACTION_TYPE_LABELS: Map<InteractionType, String> = mapOf(
    InteractionType.MEETING to "Встреча",
    InteractionType.CALL to "Звонок",
    InteractionType.CORRESPONDENCE to "Переписка",
    InteractionType.REFERRAL to "Рекомендация",
)

val DIMENSION_TYPE_LABELS: Map<DimensionLinkType, String> = mapOf(
    DimensionLinkType.WORKS_AT to "Компании",
    DimensionLinkType.ATTENDED to "События",
    DimensionLinkType.MEMBER_OF to "Сообщества",
    DimensionLinkType.INVOLVED_IN to "Проекты",
    DimensionLinkType.INTERESTED_IN to "Интересы",
    DimensionLinkType.TAGGED to "Теги",
)
