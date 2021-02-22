package de.bigboot.ggtools.fang.service

typealias Changelog = List<String>

interface ChangelogService {
    val changelog: Changelog
}